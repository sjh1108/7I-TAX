from unittest.mock import AsyncMock, patch

import pytest
from openai import APITimeoutError, AuthenticationError, RateLimitError
from tenacity import RetryError

from app.core.config import Settings
from app.core.exceptions import LLMAuthError, LLMRateLimitError, LLMTimeoutError
from app.services.chat_service import MAX_HISTORY_LENGTH, ChatService


@pytest.fixture
def settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


@pytest.fixture
def service(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock:
        mock.return_value = "테스트 응답"
        svc = ChatService(settings=settings)
        svc._mock_llm = mock  # 테스트에서 참조용
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
        service = ChatService(settings=settings)
        mock_ainvoke = AsyncMock(side_effect=APITimeoutError(request=None))
        with patch(
            "langchain_openai.ChatOpenAI.ainvoke",
            mock_ainvoke,
        ):
            with pytest.raises(RetryError):
                await service._call_llm([])
            assert mock_ainvoke.call_count == 3  # 3회 재시도 확인

    async def test_auth_error_raises_llm_auth_error(self, settings: Settings):
        service = ChatService(settings=settings)
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
        service = ChatService(settings=settings)
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
