class RetrievalService:
    """향후 RAG 파이프라인 도입 시 구현할 검색 서비스.

    TODO: ChromaDB 연동, 임베딩 생성, 유사도 검색 구현
    """

    async def retrieve(self, query: str, top_k: int = 3) -> list[str]:
        """주어진 쿼리와 관련된 문서 조각을 검색한다.

        현재는 빈 리스트를 반환 (RAG 미활성 상태).
        """
        return []
