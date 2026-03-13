from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "tax7i-ai"
    app_version: str = "0.1.0"
    debug: bool = False

    # GMS 프록시 (SSAFY OpenAI API)
    gms_api_key: str
    gms_base_url: str
    llm_model: str

    # RAG (향후 활성화)
    rag_enabled: bool = False
    chroma_persist_directory: str = "./data/chroma"
    embedding_model: str = "text-embedding-3-small"

    # 백엔드 API
    backend_base_url: str = "http://localhost:8080"
    backend_api_key: str = ""

    # CORS
    allowed_origins: list[str] = ["http://localhost:3000"]

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
