from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings

from app.core.config import Settings


class VectorStoreService:
    """ChromaDB 기반 벡터 저장소 서비스."""

    COLLECTION_NAME = "tax_laws"

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.embeddings = OpenAIEmbeddings(
            model=settings.embedding_model,
            openai_api_key=settings.gms_api_key,
            openai_api_base=settings.gms_base_url,
        )
        self.vectorstore = Chroma(
            collection_name=self.COLLECTION_NAME,
            embedding_function=self.embeddings,
            persist_directory=settings.chroma_persist_directory,
        )

    def add_documents(
        self,
        texts: list[str],
        metadatas: list[dict],
        ids: list[str] | None = None,
    ) -> None:
        """청크 텍스트와 메타데이터를 벡터 저장소에 추가한다."""
        self.vectorstore.add_texts(
            texts=texts,
            metadatas=metadatas,
            ids=ids,
        )

    def similarity_search(
        self,
        query: str,
        k: int = 5,
        filter: dict | None = None,
    ) -> list[dict]:
        """쿼리와 유사한 문서를 검색한다.

        Returns:
            [{"content": str, "metadata": dict, "score": float}, ...]
        """
        results = self.vectorstore.similarity_search_with_relevance_scores(
            query=query,
            k=k,
            filter=filter,
        )
        return [
            {
                "content": doc.page_content,
                "metadata": doc.metadata,
                "score": score,
            }
            for doc, score in results
        ]

    def get_collection_stats(self) -> dict:
        """컬렉션 통계를 반환한다.

        Returns:
            {"total_documents": int, "collection_name": str}
        """
        collection = self.vectorstore._collection
        return {
            "total_documents": collection.count(),
            "collection_name": self.COLLECTION_NAME,
        }

    def delete_collection(self) -> None:
        """컬렉션을 삭제한다 (재인덱싱 시 사용)."""
        self.vectorstore.delete_collection()
