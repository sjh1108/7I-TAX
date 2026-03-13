from dataclasses import dataclass

from app.services.vectorstore import VectorStoreService


@dataclass
class SearchResult:
    """검색 결과 단위.

    아키텍처 설계 문서 6.2절의 SearchResult 스펙을 따른다.
    """

    content: str    # 청크 텍스트
    metadata: dict  # {law_name, law_type, tax_type, chunk_id, ...}
    score: float    # 유사도 점수 (0.0 ~ 1.0)


class RetrievalService:
    """검색 서비스.

    Phase 1: 벡터 검색만 (ChromaDB similarity_search)
    Phase 2: 하이브리드 검색 추가 (BM25 + RRF)
    """

    def __init__(self, vectorstore_service: VectorStoreService) -> None:
        self.vectorstore = vectorstore_service

    async def retrieve(
        self,
        query: str,
        top_k: int = 5,
        metadata_filter: dict | None = None,
        search_strategy: str = "vector",  # Phase 1은 vector만 지원
    ) -> list[SearchResult]:
        """쿼리와 관련된 문서를 검색한다."""
        return await self._vector_search(query, top_k, metadata_filter)

    async def _vector_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None,
    ) -> list[SearchResult]:
        """ChromaDB 벡터 유사도 검색."""
        raw_results = self.vectorstore.similarity_search(
            query=query,
            k=top_k,
            filter=metadata_filter,
        )
        return [
            SearchResult(
                content=r["content"],
                metadata=r["metadata"],
                score=r["score"],
            )
            for r in raw_results
        ]
