from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.core.config import Settings
from app.services.chat_service import ChatService
from app.services.intent_classifier import IntentName, IntentResult
from app.services.retrieval_service import BM25Index, RetrievalService, SearchResult
from app.services.vectorstore import VectorStoreService


def _make_intent_result(
    intent: str,
    search_strategy: str,
    rag_required: bool,
    be_data_required: bool = False,
    metadata_filter: dict | None = None,
) -> IntentResult:
    return IntentResult(
        intent=intent,
        confidence=0.9,
        search_strategy=search_strategy,
        model_tier="mini",
        rag_required=rag_required,
        metadata_filter=metadata_filter or {},
        be_data_required=be_data_required,
    )


def _make_settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


def _make_retrieval_service() -> RetrievalService:
    mock_vs = MagicMock(spec=VectorStoreService)
    mock_vs.similarity_search.return_value = []
    return RetrievalService(vectorstore_service=mock_vs, bm25_index=BM25Index())


@pytest.fixture
def settings() -> Settings:
    return _make_settings()


@pytest.fixture
def retrieval_service() -> RetrievalService:
    return _make_retrieval_service()


@pytest.fixture
def mock_classifier() -> AsyncMock:
    return AsyncMock()


@pytest.fixture
def chat_service(settings, retrieval_service, mock_classifier) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "테스트 응답"
        svc = ChatService(
            settings=settings,
            retrieval_service=retrieval_service,
            intent_classifier=mock_classifier,
        )
        svc._mock_llm = mock_llm
        yield svc


class TestPipelineIntegration:
    """ChatService 파이프라인 통합 테스트."""

    async def test_general_intent_skips_rag(self, chat_service, mock_classifier, retrieval_service):
        """GENERAL 인텐트일 때 RAG 검색이 수행되지 않는다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            await chat_service.get_response("안녕하세요")
            mock_retrieve.assert_not_called()

    async def test_expense_intent_calls_retrieve_with_hybrid_be(
        self, chat_service, mock_classifier, retrieval_service
    ):
        """EXPENSE_CLASSIFICATION 인텐트일 때 hybrid_with_be_data 전략으로 retrieve가 호출된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.EXPENSE_CLASSIFICATION,
            search_strategy="hybrid_with_be_data",
            rag_required=True,
            metadata_filter={"topic": ["경비"]},
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("회식비 경비 처리")
            mock_retrieve.assert_called_once()
            call_kwargs = mock_retrieve.call_args.kwargs
            assert call_kwargs["search_strategy"] == "hybrid_with_be_data"

    async def test_concept_intent_uses_vector_search(
        self, chat_service, mock_classifier, retrieval_service
    ):
        """CONCEPT_EXPLANATION 인텐트일 때 vector 전략이 사용된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.CONCEPT_EXPLANATION,
            search_strategy="vector",
            rag_required=True,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("원천징수가 뭔가요?")
            mock_retrieve.assert_called_once()
            assert mock_retrieve.call_args.kwargs["search_strategy"] == "vector"

    async def test_tax_rate_intent_uses_metadata_filter(
        self, chat_service, mock_classifier, retrieval_service
    ):
        """TAX_RATE_LOOKUP 인텐트일 때 metadata_filter 전략이 사용된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.TAX_RATE_LOOKUP,
            search_strategy="metadata_filter",
            rag_required=True,
            metadata_filter={"topic": ["세율"]},
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("소득세 세율 알려줘")
            mock_retrieve.assert_called_once()
            call_kwargs = mock_retrieve.call_args.kwargs
            assert call_kwargs["search_strategy"] == "metadata_filter"
            assert call_kwargs["metadata_filter"] == {"topic": ["세율"]}

    async def test_comparison_intent_uses_multi_query(
        self, chat_service, mock_classifier, retrieval_service
    ):
        """COMPARISON 인텐트일 때 multi_query 전략이 사용된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.COMPARISON,
            search_strategy="multi_query",
            rag_required=True,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("간이과세 vs 일반과세 비교")
            assert mock_retrieve.call_args.kwargs["search_strategy"] == "multi_query"

    async def test_intent_prompt_applied_in_system_message(
        self, chat_service, mock_classifier
    ):
        """인텐트에 맞는 프롬프트가 LLM SystemMessage에 포함된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.TAX_RATE_LOOKUP,
            search_strategy="metadata_filter",
            rag_required=False,  # 검색 없이 프롬프트만 확인
        )
        await chat_service.get_response("세율 알려줘")
        call_args = chat_service._mock_llm.call_args[0][0]
        system_messages = [
            msg for msg in call_args
            if hasattr(msg, "type") and msg.type == "system"
        ]
        assert len(system_messages) >= 1
        system_content = system_messages[0].content
        assert "세율" in system_content or "한국 세금 전문가" in system_content

    async def test_search_results_included_in_context(
        self, chat_service, mock_classifier, retrieval_service
    ):
        """검색 결과가 시스템 프롬프트 컨텍스트에 포함된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.TAX_RATE_LOOKUP,
            search_strategy="metadata_filter",
            rag_required=True,
        )
        mock_results = [
            SearchResult(
                content="소득세법 제55조 세율 규정",
                metadata={"law_name": "소득세법", "law_type": "법률", "chunk_id": "c1"},
                score=0.9,
            )
        ]
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock, return_value=mock_results
        ):
            await chat_service.get_response("소득세 세율 알려줘")
            call_args = chat_service._mock_llm.call_args[0][0]
            system_msg = next(
                msg for msg in call_args
                if hasattr(msg, "type") and msg.type == "system"
            )
            assert "소득세법 제55조" in system_msg.content

    async def test_rag_not_required_skips_retrieve(
        self, chat_service, mock_classifier, retrieval_service
    ):
        """rag_required=False인 인텐트에서 retrieve가 호출되지 않는다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            await chat_service.get_response("감사합니다")
            mock_retrieve.assert_not_called()

    async def test_session_id_preserved(self, chat_service, mock_classifier):
        """기존 session_id가 유지된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        _, sid = await chat_service.get_response("안녕", session_id="my-session")
        assert sid == "my-session"

    async def test_history_preserved_across_calls(self, chat_service, mock_classifier):
        """연속 호출 시 히스토리가 누적된다."""
        mock_classifier.classify.return_value = _make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        _, sid = await chat_service.get_response("첫 번째", session_id="hist-session")
        await chat_service.get_response("두 번째", session_id=sid)

        history = chat_service.get_history(sid)
        assert len(history) == 4  # (user+ai) * 2

    async def test_all_intents_rag_routing(self, chat_service, mock_classifier, retrieval_service):
        """8개 인텐트 전체에 대해 rag_required 라우팅이 올바르게 동작한다."""
        rag_intents = [
            (IntentName.TAX_RATE_LOOKUP, "metadata_filter", True),
            (IntentName.EXPENSE_CLASSIFICATION, "hybrid_with_be_data", True),
            (IntentName.DEDUCTION_ELIGIBILITY, "hybrid", True),
            (IntentName.PROCEDURE_GUIDE, "hybrid", True),
            (IntentName.CONCEPT_EXPLANATION, "vector", True),
            (IntentName.CALCULATION, "hybrid_with_be_data", True),
            (IntentName.COMPARISON, "multi_query", True),
            (IntentName.GENERAL, "none", False),
        ]
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock, return_value=[]
        ) as mock_retrieve:
            for intent_name, strategy, rag_required in rag_intents:
                mock_retrieve.reset_mock()
                mock_classifier.classify.return_value = _make_intent_result(
                    intent=intent_name,
                    search_strategy=strategy,
                    rag_required=rag_required,
                )
                await chat_service.get_response(f"테스트 질문 for {intent_name}")

                if rag_required:
                    mock_retrieve.assert_called_once()
                    assert mock_retrieve.call_args.kwargs["search_strategy"] == strategy
                else:
                    mock_retrieve.assert_not_called()
