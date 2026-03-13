import uuid
from collections import defaultdict

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from openai import APITimeoutError, AuthenticationError, RateLimitError
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from app.core.config import Settings
from app.core.exceptions import LLMAuthError, LLMRateLimitError, LLMTimeoutError
from app.core.prompts import build_intent_prompt
from app.services.intent_classifier import IntentClassifier, IntentResult
from app.services.retrieval_service import RetrievalService
from app.utils.text_utils import format_search_results

MAX_HISTORY_LENGTH = 20


class ChatService:
    def __init__(
        self,
        settings: Settings,
        retrieval_service: RetrievalService,
        intent_classifier: IntentClassifier,
        backend_client=None,  # Phase 3: 선택적
        cache_service=None,   # Phase 4: 선택적
    ) -> None:
        self.settings = settings
        self.llm = ChatOpenAI(
            base_url=settings.gms_base_url,
            api_key=settings.gms_api_key,
            model=settings.llm_model,
            temperature=0.7,
            timeout=30,
        )
        self.retrieval_service = retrieval_service
        self.intent_classifier = intent_classifier
        self.backend_client = backend_client
        self.cache_service = cache_service
        self._histories: dict[str, list[BaseMessage]] = defaultdict(list)

    async def get_response(
        self,
        message: str,
        session_id: str | None = None,
        user_id: str | None = None,
    ) -> tuple[str, str]:
        if session_id is None:
            session_id = uuid.uuid4().hex

        history = self._histories[session_id]

        # 1. 인텐트 분류
        intent_result: IntentResult = await self.intent_classifier.classify(message)

        # 2. 시맨틱 캐시 확인 (Phase 4에서 활성화)
        if self.cache_service:
            cached = await self.cache_service.get(message)
            if cached:
                return cached, session_id

        # 3. 인텐트별 검색
        context_text = ""
        if intent_result.rag_required:
            results = await self.retrieval_service.retrieve(
                query=message,
                metadata_filter=intent_result.metadata_filter or None,
                search_strategy=intent_result.search_strategy,
            )
            context_text = format_search_results(results)

        # 4. (필요시) 백엔드 데이터 보강
        user_transactions_text = ""
        user_data_text = ""
        if intent_result.be_data_required and self.backend_client and user_id:
            from app.utils.text_utils import format_business_info, format_transactions

            transactions = await self.backend_client.get_transactions(user_id)
            business_info = await self.backend_client.get_business_info(user_id)
            user_transactions_text = format_transactions(transactions)
            user_data_text = format_business_info(business_info)

        # 5. 인텐트별 프롬프트 생성
        system_prompt = build_intent_prompt(
            intent_name=intent_result.intent,
            context=context_text,
            user_transactions=user_transactions_text,
            user_data=user_data_text,
        )

        # 6. 메시지 구성 + LLM 호출
        messages: list[BaseMessage] = [SystemMessage(content=system_prompt)]
        messages.extend(history)
        messages.append(HumanMessage(content=message))

        answer = await self._call_llm(messages)

        # 7. 히스토리 관리
        history.append(HumanMessage(content=message))
        history.append(AIMessage(content=answer))

        if len(history) > MAX_HISTORY_LENGTH:
            self._histories[session_id] = history[-MAX_HISTORY_LENGTH:]

        # 8. 캐시 저장 (Phase 4에서 활성화)
        if self.cache_service:
            await self.cache_service.put(message, answer, intent_result.intent)

        return answer, session_id

    def get_history(self, session_id: str) -> list[dict[str, str]]:
        history = self._histories.get(session_id, [])
        return [
            {
                "role": "user" if isinstance(msg, HumanMessage) else "assistant",
                "content": msg.content,
            }
            for msg in history
        ]

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type(LLMTimeoutError),
    )
    async def _call_llm(self, messages: list[BaseMessage]) -> str:
        try:
            response = await self.llm.ainvoke(messages)
            return response.content
        except APITimeoutError as e:
            raise LLMTimeoutError() from e
        except AuthenticationError as e:
            raise LLMAuthError() from e
        except RateLimitError as e:
            raise LLMRateLimitError() from e
