import uuid
from collections import defaultdict

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from openai import APITimeoutError, AuthenticationError, RateLimitError
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from app.core.config import Settings
from app.core.exceptions import LLMAuthError, LLMRateLimitError, LLMTimeoutError
from app.core.prompts import SYSTEM_PROMPT
from app.services.retrieval_service import RetrievalService, SearchResult
from app.utils.text_utils import format_search_results

MAX_HISTORY_LENGTH = 20


class ChatService:
    def __init__(self, settings: Settings, retrieval_service: RetrievalService) -> None:
        self.settings = settings
        self.llm = ChatOpenAI(
            base_url=settings.gms_base_url,
            api_key=settings.gms_api_key,
            model=settings.llm_model,
            temperature=0.7,
            timeout=30,
        )
        self.retrieval_service = retrieval_service
        self._histories: dict[str, list[BaseMessage]] = defaultdict(list)

    async def get_response(
        self,
        message: str,
        session_id: str | None = None,
    ) -> tuple[str, str]:
        if session_id is None:
            session_id = uuid.uuid4().hex

        history = self._histories[session_id]

        messages: list[BaseMessage] = [SystemMessage(content=SYSTEM_PROMPT)]

        # RAG 연동 포인트
        if self.settings.rag_enabled:
            results: list[SearchResult] = await self.retrieval_service.retrieve(
                query=message,
                top_k=5,
            )
            if results:
                context_text = format_search_results(results)
                messages.append(SystemMessage(content=f"참고 자료:\n{context_text}"))

        messages.extend(history)
        messages.append(HumanMessage(content=message))

        answer = await self._call_llm(messages)

        history.append(HumanMessage(content=message))
        history.append(AIMessage(content=answer))

        if len(history) > MAX_HISTORY_LENGTH:
            self._histories[session_id] = history[-MAX_HISTORY_LENGTH:]

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
