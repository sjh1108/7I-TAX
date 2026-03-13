from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from openai import APITimeoutError, AuthenticationError, RateLimitError
from tenacity import RetryError

from app.core.config import Settings
from app.core.exceptions import LLMAuthError, LLMRateLimitError, LLMTimeoutError
from app.services.chat_service import MAX_HISTORY_LENGTH, ChatService
from app.services.retrieval_service import BM25Index, RetrievalService, SearchResult
from app.services.vectorstore import VectorStoreService


@pytest.fixture
def settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


@pytest.fixture
def rag_settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
        rag_enabled=True,
    )


def _make_retrieval_service() -> RetrievalService:
    mock_vs = MagicMock(spec=VectorStoreService)
    mock_vs.similarity_search.return_value = []
    return RetrievalService(vectorstore_service=mock_vs, bm25_index=BM25Index())


@pytest.fixture
def service(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock:
        mock.return_value = "테스트 응답"
        svc = ChatService(settings=settings, retrieval_service=_make_retrieval_service())
        svc._mock_llm = mock
        yield svc


@pytest.fixture
def rag_service(rag_settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock:
        mock.return_value = "RAG 테스트 응답"
        svc = ChatService(settings=rag_settings, retrieval_service=_make_retrieval_service())
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


class TestCallLlm:
    """_call_llm의 예외 매핑 테스트 (ChatOpenAI 클래스 레벨 mock)"""

    async def test_timeout_retries_then_raises(self, settings: Settings):
        """타임아웃 시 3회 재시도 후 RetryError 발생 (내부에 LLMTimeoutError 포함)"""
        service = ChatService(settings=settings, retrieval_service=_make_retrieval_service())
        mock_ainvoke = AsyncMock(side_effect=APITimeoutError(request=None))
        with patch(
            "langchain_openai.ChatOpenAI.ainvoke",
            mock_ainvoke,
        ):
            with pytest.raises(RetryError):
                await service._call_llm([])
            assert mock_ainvoke.call_count == 3  # 3회 재시도 확인

    async def test_auth_error_raises_llm_auth_error(self, settings: Settings):
        service = ChatService(settings=settings, retrieval_service=_make_retrieval_service())
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
        service = ChatService(settings=settings, retrieval_service=_make_retrieval_service())
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


class TestRagIntegration:
    """RAG 연동 포인트 관련 테스트"""

    async def test_rag_disabled_does_not_call_retrieve(self, service: ChatService):
        """rag_enabled=False일 때 RetrievalService.retrieve가 호출되지 않는다."""
        with patch.object(
            service.retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            await service.get_response("안녕하세요")
            mock_retrieve.assert_not_called()

    async def test_rag_disabled_preserves_existing_behavior(self, service: ChatService):
        """rag_enabled=False일 때 기존 동작에 영향이 없다."""
        answer, session_id = await service.get_response("안녕하세요")
        assert answer == "테스트 응답"
        assert session_id is not None

    async def test_rag_enabled_calls_retrieve(self, rag_service: ChatService):
        """rag_enabled=True일 때 RetrievalService.retrieve가 호출된다."""
        with patch.object(
            rag_service.retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await rag_service.get_response("종합소득세 알려줘")
            mock_retrieve.assert_called_once_with(query="종합소득세 알려줘", top_k=5)

    async def test_rag_enabled_with_context_adds_system_message(
        self, rag_service: ChatService
    ):
        """rag_enabled=True + context 반환 시 SystemMessage에 '참고 자료'가 포함된다."""
        mock_results = [
            SearchResult(content="세금 문서 조각 1", metadata={"law_name": "소득세법", "chunk_id": "c1"}, score=0.9),
            SearchResult(content="세금 문서 조각 2", metadata={"law_name": "소득세법", "chunk_id": "c2"}, score=0.8),
        ]
        with patch.object(
            rag_service.retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = mock_results
            await rag_service.get_response("종합소득세 알려줘")

            call_args = rag_service._mock_llm.call_args[0][0]
            context_messages = [
                msg for msg in call_args
                if hasattr(msg, "content") and "참고 자료" in msg.content
            ]
            assert len(context_messages) == 1
            assert "세금 문서 조각 1" in context_messages[0].content
            assert "세금 문서 조각 2" in context_messages[0].content

    async def test_rag_enabled_empty_context_no_extra_message(
        self, rag_service: ChatService
    ):
        """rag_enabled=True이지만 context가 빈 리스트면 '참고 자료' 메시지가 추가되지 않는다."""
        with patch.object(
            rag_service.retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await rag_service.get_response("안녕")

            call_args = rag_service._mock_llm.call_args[0][0]
            context_messages = [
                msg for msg in call_args
                if hasattr(msg, "content") and "참고 자료" in msg.content
            ]
            assert len(context_messages) == 0
