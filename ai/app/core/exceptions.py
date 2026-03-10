class AIServiceError(Exception):
    def __init__(self, message: str, status_code: int = 500):
        self.message = message
        self.status_code = status_code


class LLMTimeoutError(AIServiceError):
    def __init__(self, message: str = "LLM API 응답 시간 초과"):
        super().__init__(message, status_code=504)


class LLMRateLimitError(AIServiceError):
    def __init__(self, message: str = "API 요청 한도 초과"):
        super().__init__(message, status_code=429)


class LLMAuthError(AIServiceError):
    def __init__(self, message: str = "API 인증 실패"):
        super().__init__(message, status_code=401)
