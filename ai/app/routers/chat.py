from fastapi import APIRouter, Depends

from app.core.dependencies import get_chat_service
from app.models.chat import ChatHistoryResponse, ChatRequest, ChatResponse
from app.services.chat_service import ChatService

router = APIRouter(prefix="/api/v1/chat", tags=["chat"])


@router.post("/", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    service: ChatService = Depends(get_chat_service),
) -> ChatResponse:
    answer, session_id = await service.get_response(request.message, request.session_id)
    return ChatResponse(
        answer=answer, model=service.llm_mini.model_name, session_id=session_id
    )


@router.get("/history/{session_id}", response_model=ChatHistoryResponse)
async def get_history(
    session_id: str,
    service: ChatService = Depends(get_chat_service),
) -> ChatHistoryResponse:
    messages = service.get_history(session_id)
    return ChatHistoryResponse(session_id=session_id, messages=messages, message_count=len(messages),)
