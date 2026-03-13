from dataclasses import dataclass

from rank_bm25 import BM25Okapi

from app.services.vectorstore import VectorStoreService


@dataclass
class SearchResult:
    """검색 결과 단위.

    아키텍처 설계 문서 6.2절의 SearchResult 스펙을 따른다.
    """

    content: str    # 청크 텍스트
    metadata: dict  # {law_name, law_type, tax_type, chunk_id, ...}
    score: float    # 유사도 점수 (0.0 ~ 1.0)


class BM25Index:
    """BM25 인메모리 인덱스.

    앱 기동 시 ChromaDB에서 전체 청크를 로드하여 구축한다.
    법률 용어의 키워드 정확 매칭에 사용된다.
    """

    def __init__(self) -> None:
        self.bm25: BM25Okapi | None = None
        self.documents: list[dict] = []

    def build(self, documents: list[dict]) -> None:
        """BM25 인덱스를 구축한다.

        documents: [{"content": str, "metadata": dict}, ...]
        """
        self.documents = documents
        tokenized = [doc["content"].split() for doc in documents]
        self.bm25 = BM25Okapi(tokenized)

    def search(self, query: str, top_k: int = 20) -> list[tuple[dict, float]]:
        """BM25 키워드 검색을 수행한다.

        출력: [(document_dict, score), ...] 상위 top_k개
        """
        if self.bm25 is None:
            return []

        tokenized_query = query.split()
        scores = self.bm25.get_scores(tokenized_query)
        top_indices = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)[:top_k]
        return [(self.documents[i], float(scores[i])) for i in top_indices if scores[i] > 0]

    @property
    def is_built(self) -> bool:
        return self.bm25 is not None


class RetrievalService:
    """검색 서비스.

    Phase 1: 벡터 검색만 (ChromaDB similarity_search)
    Phase 2: 하이브리드 검색 추가 (BM25 + RRF)
    """

    RRF_K = 60

    def __init__(
        self,
        vectorstore_service: VectorStoreService,
        bm25_index: BM25Index | None = None,
    ) -> None:
        self.vectorstore = vectorstore_service
        self.bm25_index = bm25_index or BM25Index()

    async def retrieve(
        self,
        query: str,
        top_k: int = 5,
        metadata_filter: dict | None = None,
        search_strategy: str = "hybrid",
    ) -> list[SearchResult]:
        """인텐트에 따른 검색 전략 분기.

        search_strategy 옵션:
        - "hybrid": BM25 + 벡터 + RRF (기본)
        - "vector": 벡터 검색만
        - "metadata_filter": 메타데이터 필터 중심
        - "multi_query": 비교 대상별 다중 쿼리
        - "none": 검색 생략
        """
        if search_strategy == "none":
            return []

        if search_strategy == "vector":
            return await self._vector_search(query, top_k, metadata_filter)

        if search_strategy == "metadata_filter":
            return await self._filtered_search(query, top_k, metadata_filter)

        if search_strategy == "multi_query":
            return await self._multi_query_search(query, top_k)

        return await self._hybrid_search(query, top_k, metadata_filter)

    async def _hybrid_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None,
    ) -> list[SearchResult]:
        """BM25 + 벡터 + RRF 하이브리드 검색."""
        bm25_results = self._bm25_search(query, top_k=20, metadata_filter=metadata_filter)
        vector_results = await self._vector_search(query, top_k=20, metadata_filter=metadata_filter)
        return self._rrf_fusion(bm25_results, vector_results, top_k)

    async def _filtered_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None,
    ) -> list[SearchResult]:
        """메타데이터 필터 중심 검색 (TAX_RATE_LOOKUP)."""
        return await self._vector_search(query, top_k, metadata_filter)

    async def _multi_query_search(
        self,
        query: str,
        top_k: int,
    ) -> list[SearchResult]:
        """다중 쿼리 검색 (COMPARISON). MVP: 단일 쿼리로 검색."""
        return await self._vector_search(query, top_k, None)

    def _bm25_search(
        self,
        query: str,
        top_k: int = 20,
        metadata_filter: dict | None = None,
    ) -> list[SearchResult]:
        """BM25 키워드 검색 + 메타데이터 필터링."""
        raw = self.bm25_index.search(query, top_k=top_k * 2)
        if metadata_filter:
            raw = self._apply_metadata_filter(raw, metadata_filter)
        raw = raw[:top_k]
        return [
            SearchResult(
                content=doc["content"],
                metadata=doc["metadata"],
                score=score,
            )
            for doc, score in raw
        ]

    def _apply_metadata_filter(
        self,
        results: list[tuple[dict, float]],
        metadata_filter: dict | None,
    ) -> list[tuple[dict, float]]:
        """메타데이터 필터를 적용한다.

        지원 연산자:
        - {"$in": [v1, v2]}: metadata[key] in values
        - {"$contains": v}: v in metadata[key]
        - 단일 값: metadata[key] == value
        """
        if metadata_filter is None:
            return results

        filtered = []
        for doc, score in results:
            meta = doc.get("metadata", {})
            match = True
            for key, condition in metadata_filter.items():
                if isinstance(condition, dict):
                    if "$in" in condition:
                        if meta.get(key) not in condition["$in"]:
                            match = False
                            break
                    elif "$contains" in condition:
                        if condition["$contains"] not in str(meta.get(key, "")):
                            match = False
                            break
                else:
                    if meta.get(key) != condition:
                        match = False
                        break
            if match:
                filtered.append((doc, score))

        return filtered

    def _rrf_fusion(
        self,
        results_a: list[SearchResult],
        results_b: list[SearchResult],
        top_k: int,
    ) -> list[SearchResult]:
        """Reciprocal Rank Fusion.

        공식: score(d) = sum(1 / (k + rank_i))
        """
        scores: dict[str, float] = {}
        docs: dict[str, SearchResult] = {}

        for rank, result in enumerate(results_a):
            doc_id = result.metadata.get("chunk_id", result.content[:50])
            scores[doc_id] = scores.get(doc_id, 0) + 1 / (self.RRF_K + rank + 1)
            docs[doc_id] = result

        for rank, result in enumerate(results_b):
            doc_id = result.metadata.get("chunk_id", result.content[:50])
            scores[doc_id] = scores.get(doc_id, 0) + 1 / (self.RRF_K + rank + 1)
            docs[doc_id] = result

        sorted_ids = sorted(scores, key=scores.__getitem__, reverse=True)[:top_k]
        return [
            SearchResult(
                content=docs[doc_id].content,
                metadata=docs[doc_id].metadata,
                score=scores[doc_id],
            )
            for doc_id in sorted_ids
        ]

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
