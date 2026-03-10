from app.services.retrieval_service import RetrievalService


class TestRetrievalService:
    async def test_import_and_instantiate(self):
        """RetrievalService를 import하고 인스턴스를 생성할 수 있다."""
        service = RetrievalService()
        assert service is not None

    async def test_retrieve_returns_empty_list(self):
        """스텁 상태에서 retrieve()는 빈 리스트를 반환한다."""
        service = RetrievalService()
        result = await service.retrieve("세금 관련 질문")
        assert result == []

    async def test_retrieve_with_custom_top_k(self):
        """top_k 파라미터를 전달해도 빈 리스트를 반환한다."""
        service = RetrievalService()
        result = await service.retrieve("종합소득세", top_k=5)
        assert result == []
