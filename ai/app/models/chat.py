from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000, description="사용자 메시지")
    session_id: str | None = Field(default=None, description="세션 ID")


class ChatResponse(BaseModel):
    answer: str = Field(..., description="AI 응답")
    model: str = Field(..., description="사용된 모델명")
    session_id: str = Field(..., description="세션 ID")
    usage: dict | None = Field(default=None, description="토큰 사용량")


class ChatHistoryResponse(BaseModel):
    session_id: str
    messages: list[dict[str, str]]
    message_count: int
