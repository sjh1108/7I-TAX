from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.core.config import Settings
from app.core.dependencies import get_chat_service
from app.main import app
from app.services.chat_service import ChatService
from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.vectorstore import VectorStoreService


@pytest.fixture
def mock_settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


@pytest.fixture
def mock_retrieval_service() -> RetrievalService:
    """VectorStoreService와 BM25Index를 Mock 처리한 RetrievalService."""
    mock_vs = MagicMock(spec=VectorStoreService)
    mock_vs.similarity_search.return_value = []
    bm25 = BM25Index()
    return RetrievalService(vectorstore_service=mock_vs, bm25_index=bm25)


@pytest.fixture
def mock_chat_service(mock_settings: Settings, mock_retrieval_service: RetrievalService) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "테스트 응답입니다."
        service = ChatService(settings=mock_settings, retrieval_service=mock_retrieval_service)
        yield service


@pytest.fixture
async def client(mock_chat_service: ChatService):
    app.dependency_overrides[get_chat_service] = lambda: mock_chat_service
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()
