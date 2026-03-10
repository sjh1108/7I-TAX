from functools import lru_cache

from fastapi import Depends

from app.core.config import Settings
from app.services.chat_service import ChatService


@lru_cache
def get_settings() -> Settings:
    return Settings()


def get_chat_service(
    settings: Settings = Depends(get_settings),
) -> ChatService:
    return ChatService(settings=settings)
