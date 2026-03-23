import numpy as np
from langchain_openai import OpenAIEmbeddings

from app.core.config import Settings


class EmbeddingService:
    """텍스트 임베딩 생성 서비스.

    OpenAI text-embedding-3-small 모델 사용.
    GMS 프록시를 통해 API 호출.
    """

    def __init__(self, settings: Settings) -> None:
        self.embeddings = OpenAIEmbeddings(
            model=settings.embedding_model,
            openai_api_key=settings.gms_api_key,
            openai_api_base=settings.gms_base_url,
        )

    async def embed_text(self, text: str) -> list[float]:
        """단일 텍스트의 임베딩 벡터를 생성한다.

        출력: list[float] — 1536차원 벡터 (text-embedding-3-small)
        """
        return await self.embeddings.aembed_query(text)

    async def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """여러 텍스트의 임베딩 벡터를 배치로 생성한다."""
        return await self.embeddings.aembed_documents(texts)

    @staticmethod
    def cosine_similarity(vec_a: list[float], vec_b: list[float]) -> float:
        """두 벡터 간 코사인 유사도를 계산한다.

        출력: float (0.0 ~ 1.0)
        """
        a = np.array(vec_a)
        b = np.array(vec_b)
        return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)))
