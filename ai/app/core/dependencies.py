from functools import lru_cache

from app.core.config import Settings
from app.services.chat_service import ChatService
from app.services.retrieval_service import RetrievalService
from app.services.vectorstore import VectorStoreService

_vectorstore_service: VectorStoreService | None = None
_retrieval_service: RetrievalService | None = None
_chat_service: ChatService | None = None


@lru_cache
def get_settings() -> Settings:
    return Settings()


def init_services() -> None:
    """모든 서비스를 초기화한다. 앱 lifespan에서 호출.

    초기화 순서 (의존성 순):
    1. VectorStoreService (ChromaDB 연결)
    2. RetrievalService (VectorStoreService 주입)
    3. ChatService (Settings + RetrievalService 주입)
    """
    global _vectorstore_service, _retrieval_service, _chat_service

    _vectorstore_service = VectorStoreService(get_settings())
    _retrieval_service = RetrievalService(vectorstore_service=_vectorstore_service)
    _chat_service = ChatService(settings=get_settings(), retrieval_service=_retrieval_service)


def get_chat_service() -> ChatService:
    """ChatService 인스턴스를 반환한다. FastAPI Depends()에서 사용."""
    if _chat_service is None:
        raise RuntimeError("서비스가 초기화되지 않았습니다. init_services()를 먼저 호출하세요.")
    return _chat_service
