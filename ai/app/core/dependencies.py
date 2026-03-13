import logging
from functools import lru_cache

from app.core.config import Settings
from app.services.chat_service import ChatService
from app.services.embedding_service import EmbeddingService
from app.services.intent_classifier import IntentClassifier
from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.vectorstore import VectorStoreService

logger = logging.getLogger(__name__)

_vectorstore_service: VectorStoreService | None = None
_retrieval_service: RetrievalService | None = None
_chat_service: ChatService | None = None

INTENTS_PATH = "app/data/intents/tax_intents.json"


@lru_cache
def get_settings() -> Settings:
    return Settings()


async def init_services() -> None:
    """모든 서비스를 초기화한다. 앱 lifespan에서 호출.

    초기화 순서 (의존성 순):
    1. VectorStoreService (ChromaDB 연결)
    2. BM25Index 구축 (ChromaDB 전체 문서 로드)
    3. RetrievalService (VectorStoreService + BM25Index 주입)
    4. EmbeddingService + IntentClassifier (예시 발화 임베딩)
    5. ChatService (Settings + RetrievalService + IntentClassifier 주입)
    """
    global _vectorstore_service, _retrieval_service, _chat_service

    settings = get_settings()

    _vectorstore_service = VectorStoreService(settings)

    bm25_index = BM25Index()
    all_docs = _vectorstore_service.get_all_documents()
    if all_docs:
        bm25_index.build(all_docs)
        logger.info("BM25 인덱스 구축 완료: %d개 문서", len(all_docs))
    else:
        logger.warning("BM25 인덱스: 문서 없음 (ChromaDB 비어있음)")

    _retrieval_service = RetrievalService(
        vectorstore_service=_vectorstore_service,
        bm25_index=bm25_index,
    )

    embedding_service = EmbeddingService(settings)
    intent_classifier = IntentClassifier(INTENTS_PATH, embedding_service)
    await intent_classifier.initialize()
    logger.info("IntentClassifier 초기화 완료")

    _chat_service = ChatService(
        settings=settings,
        retrieval_service=_retrieval_service,
        intent_classifier=intent_classifier,
    )


def get_chat_service() -> ChatService:
    """ChatService 인스턴스를 반환한다. FastAPI Depends()에서 사용."""
    if _chat_service is None:
        raise RuntimeError("서비스가 초기화되지 않았습니다. init_services()를 먼저 호출하세요.")
    return _chat_service
