from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage

from app.core.config import Settings
from app.core.prompts import SYSTEM_PROMPT


class ChatService:
    def __init__(self, settings: Settings) -> None:
        self.llm = ChatOpenAI(
            base_url=settings.gms_base_url,
            api_key=settings.gms_api_key,
            model=settings.llm_model,
            temperature=0.7,
        )

    async def get_response(
        self,
        message: str,
        history: list[dict] | None = None,
    ) -> str:
        messages = [SystemMessage(content=SYSTEM_PROMPT)]

        if history:
            for msg in history:
                if msg["role"] == "user":
                    messages.append(HumanMessage(content=msg["content"]))
                else:
                    from langchain_core.messages import AIMessage
                    messages.append(AIMessage(content=msg["content"]))

        messages.append(HumanMessage(content=message))

        response = await self.llm.ainvoke(messages)
        return response.content
