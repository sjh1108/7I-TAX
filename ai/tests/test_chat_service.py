from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from openai import APITimeoutError, AuthenticationError, RateLimitError
from tenacity import RetryError

from app.core.config import Settings
from app.core.exceptions import LLMAuthError, LLMRateLimitError, LLMTimeoutError
from app.services.chat_service import MAX_HISTORY_LENGTH, ChatService
from app.services.intent_classifier import IntentName, IntentResult
from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.vectorstore import VectorStoreService


@pytest.fixture
def settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


def _make_retrieval_service() -> RetrievalService:
    mock_vs = MagicMock(spec=VectorStoreService)
    mock_vs.similarity_search.return_value = []
    return RetrievalService(vectorstore_service=mock_vs, bm25_index=BM25Index())


def _make_mock_classifier(
    intent: str = IntentName.GENERAL,
    rag_required: bool = False,
) -> AsyncMock:
    mock = AsyncMock()
    mock.classify.return_value = IntentResult(
        intent=intent,
        confidence=0.9,
        search_strategy="none",
        model_tier="mini",
        rag_required=rag_required,
        metadata_filter={},
    )
    return mock


@pytest.fixture
def service(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock:
        mock.return_value = "테스트 응답"
        svc = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=_make_mock_classifier(),
        )
        svc._mock_llm = mock
        yield svc


class TestGetResponse:
    async def test_new_session_creates_uuid(self, service: ChatService):
        _, session_id = await service.get_response("안녕")
        assert session_id is not None
        assert len(session_id) == 32  # UUID hex

    async def test_existing_session_reused(self, service: ChatService):
        _, session_id = await service.get_response("첫 메시지", session_id="my-session")
        assert session_id == "my-session"

    async def test_history_preserved_across_calls(self, service: ChatService):
        _, sid = await service.get_response("첫 번째")
        await service.get_response("두 번째", session_id=sid)

        history = service.get_history(sid)
        assert len(history) == 4  # user+ai * 2

    async def test_history_trimmed_at_max_length(self, service: ChatService):
        sid = "trim-test"
        for i in range(MAX_HISTORY_LENGTH):
            await service.get_response(f"메시지 {i}", session_id=sid)

        history = service.get_history(sid)
        assert len(history) <= MAX_HISTORY_LENGTH


class TestGetHistory:
    async def test_returns_formatted_messages(self, service: ChatService):
        await service.get_response("안녕", session_id="hist-test")
        history = service.get_history("hist-test")

        assert history[0] == {"role": "user", "content": "안녕"}
        assert history[1]["role"] == "assistant"

    async def test_unknown_session_returns_empty(self, service: ChatService):
        history = service.get_history("nonexistent")
        assert history == []


@pytest.fixture
def service_with_cache(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "파이프라인 답변"
        cache_svc = AsyncMock()
        cache_svc.get = AsyncMock(return_value=None)
        cache_svc.put = AsyncMock()
        svc = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=_make_mock_classifier(),
            cache_service=cache_svc,
        )
        svc._mock_llm = mock_llm
        yield svc


@pytest.fixture
def service_with_backend(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "답변"
        mock_backend = AsyncMock()
        mock_backend.get_transactions = AsyncMock(return_value=[])
        mock_backend.get_business_info = AsyncMock(return_value=None)
        clf = AsyncMock()
        clf.classify.return_value = IntentResult(
            intent=IntentName.EXPENSE_CLASSIFICATION,
            confidence=0.9,
            search_strategy="none",
            model_tier="standard",
            rag_required=False,
            metadata_filter={},
            be_data_required=True,
        )
        svc = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=clf,
            backend_client=mock_backend,
        )
        svc._mock_llm = mock_llm
        yield svc


@pytest.fixture
def service_without_backend(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "답변"
        clf = AsyncMock()
        clf.classify.return_value = IntentResult(
            intent=IntentName.EXPENSE_CLASSIFICATION,
            confidence=0.9,
            search_strategy="none",
            model_tier="mini",
            rag_required=False,
            metadata_filter={},
            be_data_required=True,
        )
        svc = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=clf,
            backend_client=None,
        )
        svc._mock_llm = mock_llm
        yield svc


# ---------------------------------------------------------------------------
# 캐시 통합 경로 테스트
# ---------------------------------------------------------------------------

class TestCacheIntegration:
    async def test_cache_hit_returns_early_without_pipeline(
        self, service_with_cache: ChatService
    ):
        """캐시 히트 시 _call_llm이 호출되지 않아야 한다."""
        service_with_cache.cache_service.get.return_value = "캐시된 답변"

        answer, _ = await service_with_cache.get_response(
            "세금 환급은 어떻게 하나요?", session_id="s1"
        )

        assert answer == "캐시된 답변"
        service_with_cache._mock_llm.assert_not_called()

    async def test_cache_miss_proceeds_to_pipeline(
        self, service_with_cache: ChatService
    ):
        """캐시 미스 시 _call_llm이 호출되어야 한다."""
        service_with_cache.cache_service.get.return_value = None

        answer, _ = await service_with_cache.get_response(
            "부가세 신고 방법은?", session_id="s2"
        )

        service_with_cache._mock_llm.assert_called_once()
        assert answer == "파이프라인 답변"

    async def test_response_is_stored_in_cache_after_pipeline(
        self, service_with_cache: ChatService
    ):
        """파이프라인 실행 후 cache_service.put()이 호출되어야 한다."""
        service_with_cache.cache_service.get.return_value = None
        question = "종합소득세 신고 기한은?"

        await service_with_cache.get_response(question, session_id="s3")

        service_with_cache.cache_service.put.assert_called_once()
        call_args = service_with_cache.cache_service.put.call_args
        assert call_args[0][0] == question


# ---------------------------------------------------------------------------
# be_data_required 분기 테스트
# ---------------------------------------------------------------------------

class TestBeDataBranch:
    async def test_be_data_required_true_calls_backend_apis(
        self, service_with_backend: ChatService
    ):
        """be_data_required=True 인텐트에서 get_transactions, get_business_info가 호출된다."""
        await service_with_backend.get_response(
            "내 경비 공제 내역 알려줘", session_id="s1", user_id="user-42"
        )

        service_with_backend.backend_client.get_transactions.assert_called_once_with(
            "user-42"
        )
        service_with_backend.backend_client.get_business_info.assert_called_once_with(
            "user-42"
        )

    async def test_no_user_id_skips_backend_call(
        self, service_with_backend: ChatService
    ):
        """user_id가 없으면 be_data_required=True여도 백엔드를 호출하지 않는다."""
        await service_with_backend.get_response(
            "내 경비 공제 내역 알려줘", session_id="s1"
        )

        service_with_backend.backend_client.get_transactions.assert_not_called()
        service_with_backend.backend_client.get_business_info.assert_not_called()

    async def test_no_backend_client_proceeds_with_empty_data(
        self, service_without_backend: ChatService
    ):
        """backend_client 미주입 시 be_data_required=True여도 예외 없이 진행된다."""
        result = await service_without_backend.get_response(
            "경비 알려줘", session_id="s1", user_id="user-1"
        )

        assert result is not None
        answer, _ = result
        assert answer == "답변"
        service_without_backend._mock_llm.assert_called_once()


class TestCallLlm:
    """_call_llm의 예외 매핑 테스트"""

    async def test_timeout_retries_then_raises(self, settings: Settings):
        """타임아웃 시 3회 재시도 후 RetryError 발생"""
        service = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=_make_mock_classifier(),
        )
        mock_ainvoke = AsyncMock(side_effect=APITimeoutError(request=None))
        with patch("langchain_openai.ChatOpenAI.ainvoke", mock_ainvoke):
            with pytest.raises(RetryError):
                await service._call_llm([])
            assert mock_ainvoke.call_count == 3

    async def test_auth_error_raises_llm_auth_error(self, settings: Settings):
        service = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=_make_mock_classifier(),
        )
        mock_response = AsyncMock()
        mock_response.status_code = 401
        mock_response.json.return_value = {"error": {"message": "invalid key"}}
        with patch(
            "langchain_openai.ChatOpenAI.ainvoke",
            new_callable=AsyncMock,
            side_effect=AuthenticationError(
                message="auth failed", response=mock_response, body=None
            ),
        ):
            with pytest.raises(LLMAuthError):
                await service._call_llm([])

    async def test_rate_limit_raises_llm_rate_limit_error(self, settings: Settings):
        service = ChatService(
            settings=settings,
            retrieval_service=_make_retrieval_service(),
            intent_classifier=_make_mock_classifier(),
        )
        mock_response = AsyncMock()
        mock_response.status_code = 429
        mock_response.json.return_value = {"error": {"message": "rate limit"}}
        with patch(
            "langchain_openai.ChatOpenAI.ainvoke",
            new_callable=AsyncMock,
            side_effect=RateLimitError(
                message="rate limit", response=mock_response, body=None
            ),
        ):
            with pytest.raises(LLMRateLimitError):
                await service._call_llm([])
