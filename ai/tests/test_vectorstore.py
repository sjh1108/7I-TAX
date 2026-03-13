from unittest.mock import MagicMock, patch

import pytest

from app.core.config import Settings
from app.services.vectorstore import VectorStoreService


@pytest.fixture
def settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


@pytest.fixture
def mock_vectorstore_service(settings: Settings) -> VectorStoreService:
    """OpenAIEmbeddings와 Chroma를 Mock 처리한 VectorStoreService."""
    with (
        patch("app.services.vectorstore.OpenAIEmbeddings") as mock_embeddings_cls,
        patch("app.services.vectorstore.Chroma") as mock_chroma_cls,
    ):
        mock_embeddings_cls.return_value = MagicMock()

        mock_collection = MagicMock()
        mock_collection.count.return_value = 10

        mock_chroma = MagicMock()
        mock_chroma._collection = mock_collection
        mock_chroma_cls.return_value = mock_chroma

        service = VectorStoreService(settings=settings)
        service._mock_chroma = mock_chroma
        service._mock_collection = mock_collection
        yield service


class TestVectorStoreServiceInit:
    def test_instantiation_with_correct_settings(self, settings: Settings):
        """Settings를 전달하면 VectorStoreService 인스턴스가 생성된다."""
        with (
            patch("app.services.vectorstore.OpenAIEmbeddings"),
            patch("app.services.vectorstore.Chroma"),
        ):
            service = VectorStoreService(settings=settings)
            assert service is not None

    def test_openai_embeddings_called_with_correct_params(self, settings: Settings):
        """OpenAIEmbeddings가 Settings의 값으로 초기화된다."""
        with (
            patch("app.services.vectorstore.OpenAIEmbeddings") as mock_embeddings_cls,
            patch("app.services.vectorstore.Chroma"),
        ):
            VectorStoreService(settings=settings)
            mock_embeddings_cls.assert_called_once_with(
                model=settings.embedding_model,
                openai_api_key=settings.gms_api_key,
                openai_api_base=settings.gms_base_url,
            )

    def test_chroma_called_with_correct_params(self, settings: Settings):
        """Chroma가 올바른 collection_name과 persist_directory로 초기화된다."""
        with (
            patch("app.services.vectorstore.OpenAIEmbeddings") as mock_embeddings_cls,
            patch("app.services.vectorstore.Chroma") as mock_chroma_cls,
        ):
            mock_embeddings = MagicMock()
            mock_embeddings_cls.return_value = mock_embeddings

            VectorStoreService(settings=settings)

            mock_chroma_cls.assert_called_once_with(
                collection_name=VectorStoreService.COLLECTION_NAME,
                embedding_function=mock_embeddings,
                persist_directory=settings.chroma_persist_directory,
            )


class TestAddDocuments:
    def test_add_documents_without_ids(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """ids 없이 texts와 metadatas만 전달하면 add_texts가 호출된다."""
        texts = ["세금 문서 1", "세금 문서 2"]
        metadatas = [{"source": "doc1.pdf"}, {"source": "doc2.pdf"}]

        mock_vectorstore_service.add_documents(texts=texts, metadatas=metadatas)

        mock_vectorstore_service._mock_chroma.add_texts.assert_called_once_with(
            texts=texts,
            metadatas=metadatas,
            ids=None,
        )

    def test_add_documents_with_ids(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """ids를 전달하면 add_texts에 ids가 포함된다."""
        texts = ["청크 1", "청크 2"]
        metadatas = [{"page": 1}, {"page": 2}]
        ids = ["id-001", "id-002"]

        mock_vectorstore_service.add_documents(
            texts=texts, metadatas=metadatas, ids=ids
        )

        mock_vectorstore_service._mock_chroma.add_texts.assert_called_once_with(
            texts=texts,
            metadatas=metadatas,
            ids=ids,
        )

    def test_add_documents_empty_list(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """빈 리스트를 전달해도 add_texts가 호출된다."""
        mock_vectorstore_service.add_documents(texts=[], metadatas=[])

        mock_vectorstore_service._mock_chroma.add_texts.assert_called_once()


class TestSimilaritySearch:
    def test_similarity_search_returns_formatted_results(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """similarity_search_with_relevance_scores 결과를 올바른 형식으로 변환한다."""
        mock_doc1 = MagicMock()
        mock_doc1.page_content = "종합소득세 내용"
        mock_doc1.metadata = {"source": "tax.pdf", "page": 1}

        mock_doc2 = MagicMock()
        mock_doc2.page_content = "부가가치세 내용"
        mock_doc2.metadata = {"source": "vat.pdf", "page": 3}

        mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.return_value = [
            (mock_doc1, 0.92),
            (mock_doc2, 0.78),
        ]

        results = mock_vectorstore_service.similarity_search("소득세")

        assert len(results) == 2
        assert results[0]["content"] == "종합소득세 내용"
        assert results[0]["metadata"] == {"source": "tax.pdf", "page": 1}
        assert results[0]["score"] == 0.92
        assert results[1]["content"] == "부가가치세 내용"
        assert results[1]["score"] == 0.78

    def test_similarity_search_passes_k_and_filter(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """k와 filter 파라미터가 similarity_search_with_relevance_scores에 전달된다."""
        mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.return_value = []

        mock_vectorstore_service.similarity_search(
            query="부동산 양도소득세",
            k=3,
            filter={"source": "tax.pdf"},
        )

        mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.assert_called_once_with(
            query="부동산 양도소득세",
            k=3,
            filter={"source": "tax.pdf"},
        )

    def test_similarity_search_default_k(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """k 기본값은 5이다."""
        mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.return_value = []

        mock_vectorstore_service.similarity_search("세금")

        call_kwargs = mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.call_args.kwargs
        assert call_kwargs["k"] == 5

    def test_similarity_search_no_filter_passes_none(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """filter를 지정하지 않으면 None이 전달된다."""
        mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.return_value = []

        mock_vectorstore_service.similarity_search("세금")

        call_kwargs = mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.call_args.kwargs
        assert call_kwargs["filter"] is None

    def test_similarity_search_empty_results(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """결과가 없으면 빈 리스트를 반환한다."""
        mock_vectorstore_service._mock_chroma.similarity_search_with_relevance_scores.return_value = []

        results = mock_vectorstore_service.similarity_search("존재하지 않는 내용")

        assert results == []


class TestGetCollectionStats:
    def test_returns_total_documents_and_collection_name(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """컬렉션 통계에 total_documents와 collection_name이 포함된다."""
        mock_vectorstore_service._mock_collection.count.return_value = 42

        stats = mock_vectorstore_service.get_collection_stats()

        assert stats["total_documents"] == 42
        assert stats["collection_name"] == VectorStoreService.COLLECTION_NAME

    def test_collection_count_called(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """get_collection_stats 호출 시 collection.count()가 실행된다."""
        mock_vectorstore_service.get_collection_stats()

        mock_vectorstore_service._mock_collection.count.assert_called_once()

    def test_returns_zero_when_empty(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """빈 컬렉션이면 total_documents가 0이다."""
        mock_vectorstore_service._mock_collection.count.return_value = 0

        stats = mock_vectorstore_service.get_collection_stats()

        assert stats["total_documents"] == 0


class TestDeleteCollection:
    def test_delete_collection_calls_vectorstore_delete(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """delete_collection 호출 시 vectorstore.delete_collection()이 실행된다."""
        mock_vectorstore_service.delete_collection()

        mock_vectorstore_service._mock_chroma.delete_collection.assert_called_once()

    def test_delete_collection_returns_none(
        self, mock_vectorstore_service: VectorStoreService
    ):
        """delete_collection은 None을 반환한다."""
        result = mock_vectorstore_service.delete_collection()

        assert result is None
