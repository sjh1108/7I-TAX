from functools import lru_cache

from fastapi import Depends

from app.core.config import Settings
from app.services.chat_service import ChatService


@lru_cache
def get_settings() -> Settings:
    return Settings()


@lru_cache
def _create_chat_service() -> ChatService:
    return ChatService(settings=get_settings())


def get_chat_service() -> ChatService:
    return _create_chat_service()
