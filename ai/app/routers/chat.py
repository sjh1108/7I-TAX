from fastapi import APIRouter, Depends

from app.core.dependencies import get_chat_service
from app.models.chat import ChatRequest, ChatResponse
from app.services.chat_service import ChatService

router = APIRouter(prefix="/api/v1/chat", tags=["chat"])


@router.post("/", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    service: ChatService = Depends(get_chat_service),
) -> ChatResponse:
    answer = await service.get_response(request.message)
    return ChatResponse(answer=answer, model=service.llm.model_name)
