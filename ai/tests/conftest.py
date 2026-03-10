from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.core.config import Settings
from app.core.dependencies import get_chat_service
from app.main import app
from app.services.chat_service import ChatService


@pytest.fixture
def mock_settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


@pytest.fixture
def mock_chat_service(mock_settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "테스트 응답입니다."
        service = ChatService(settings=mock_settings)
        yield service


@pytest.fixture
async def client(mock_chat_service: ChatService):
    app.dependency_overrides[get_chat_service] = lambda: mock_chat_service
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()
