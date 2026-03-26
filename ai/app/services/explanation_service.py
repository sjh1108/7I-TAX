import json
import logging
import re

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

from app.core.prompts import CLASSIFY_EXPLANATION_PROMPT
from app.services.retrieval_service import RetrievalService, SearchResult

logger = logging.getLogger(__name__)


class ExplanationService:
    """거래 세목 분류 결과에 대한 법적 근거와 이유를 생성하는 서비스."""

    def __init__(
        self,
        retrieval_service: RetrievalService,
        llm: ChatOpenAI,
        category_mappings: list[dict],
    ) -> None:
        """설명 생성 서비스를 초기화한다.

        Args:
            retrieval_service: RAG 검색 서비스.
            llm: LLM 인스턴스.
            category_mappings: 세목별 검색 설정. [{"category": str, "search_keywords": [...], "law_filter": [...]}, ...].
        """
        self.retrieval_service = retrieval_service
        self.llm = llm
        self._mapping_dict: dict[str, dict] = {
            m["category"]: m for m in category_mappings
        }

    async def generate_explanation(
        self,
        description: str,
        category: str,
        confidence: float,
    ) -> dict:
        """거래 분류 결과에 대한 법적 근거와 이유를 생성한다.

        검색 실패 또는 예외 발생 시 기본 설명을 반환한다.

        Args:
            description: 거래 설명.
            category: 분류된 세목명.
            confidence: 분류 신뢰도.

        Returns:
            dict: {"reason": str, "legal_basis": str}.
        """
        try:
            mapping = self._get_mapping(category)
            if mapping is None:
                return self._default_explanation(category)

            search_query = self._build_search_query(description, mapping)
            metadata_filter = self._build_filter(mapping)
            search_results = await self.retrieval_service.retrieve(
                query=search_query,
                top_k=3,
                metadata_filter=metadata_filter,
                search_strategy="hybrid",
            )
            context = self._format_context(search_results)
            raw_response = await self._call_llm(category, description, context)
            return self._parse_response(raw_response)
        except Exception as e:
            logger.warning("설명 생성 실패 (fallback 사용): %s", e)
            return self._default_explanation(category)

    def _get_mapping(self, category: str) -> dict | None:
        """카테고리에 해당하는 검색 매핑을 조회한다.

        Args:
            category: 세목명.

        Returns:
            dict | None: 매핑 정보 또는 None.
        """
        return self._mapping_dict.get(category)

    def _build_search_query(self, description: str, mapping: dict) -> str:
        """거래 설명과 검색 키워드를 조합하여 검색 쿼리를 생성한다.

        Args:
            description: 거래 설명.
            mapping: 매핑 정보.

        Returns:
            str: 검색 쿼리.
        """
        keywords = " ".join(mapping.get("search_keywords", []))
        return f"{description} {keywords}"

    def _build_filter(self, mapping: dict) -> dict:
        """매핑 정보로부터 메타데이터 필터를 생성한다.

        Args:
            mapping: 매핑 정보.

        Returns:
            dict: 메타데이터 필터.
        """
        return {"law_name": {"$in": mapping["law_filter"]}}

    def _format_context(self, search_results: list[SearchResult]) -> str:
        """검색 결과를 텍스트 컨텍스트로 포맷한다.

        Args:
            search_results: 검색 결과 리스트.

        Returns:
            str: 포맷된 컨텍스트 텍스트.
        """
        if not search_results:
            return "(관련 법률 자료 없음)"

        parts = []
        for result in search_results:
            content = result.content[:500]
            source = result.metadata.get("law_name", "알 수 없음")
            parts.append(f"[출처: {source}]\n{content}")
        return "\n\n---\n\n".join(parts)

    async def _call_llm(self, category: str, description: str, context: str) -> str:
        """LLM에 설명 생성을 요청한다.

        Args:
            category: 세목명.
            description: 거래 설명.
            context: 검색 컨텍스트.

        Returns:
            str: LLM 응답.
        """
        prompt = CLASSIFY_EXPLANATION_PROMPT.format(
            category=category,
            description=description,
            context=context,
        )
        messages = [
            SystemMessage(content="당신은 한국 세금 전문가입니다."),
            HumanMessage(content=prompt),
        ]
        response = await self.llm.ainvoke(messages)
        return response.content

    def _parse_response(self, response: str) -> dict:
        """LLM 응답을 파싱하여 구조화된 설명으로 변환한다.

        JSON 파싱 실패 시 응답을 그대로 reason으로 사용한다.

        Args:
            response: LLM 응답 텍스트.

        Returns:
            dict: {"reason": str, "legal_basis": str}.
        """
        cleaned = re.sub(r"```(?:json)?\s*", "", response).strip()
        cleaned = cleaned.rstrip("`").strip()

        try:
            parsed = json.loads(cleaned)
            return {
                "reason": parsed.get("reason", ""),
                "legal_basis": parsed.get("legal_basis", ""),
            }
        except json.JSONDecodeError:
            logger.warning("LLM 응답 JSON 파싱 실패: %s", response[:200])
            return {"reason": response.strip()[:200], "legal_basis": ""}

    def _default_explanation(self, category: str) -> dict:
        """기본 설명을 반환한다.

        검색 또는 LLM 호출 실패 시 사용된다.

        Args:
            category: 세목명.

        Returns:
            dict: {"reason": str, "legal_basis": str}.
        """
        mapping = self._get_mapping(category)
        hint = mapping["description_hint"] if mapping else f"'{category}' 세목으로 분류되었습니다."
        return {"reason": hint, "legal_basis": ""}
