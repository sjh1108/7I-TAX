from pydantic import BaseModel, Field


class TransactionClassifyRequest(BaseModel):
    description: str = Field(
        ..., min_length=1, max_length=500, description="거래 내역 설명"
    )


class TransactionClassifyResponse(BaseModel):
    category: str = Field(..., description="분류된 세목")
    confidence: float = Field(..., description="예측 확률 (0~1)")
    method: str = Field(..., description="분류 방법 (local_model | llm_fallback)")
