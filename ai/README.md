# tax7i AI 서버

세금 관련 AI 챗봇 서비스의 백엔드 서버입니다. FastAPI 기반으로 LLM 채팅 API를 제공합니다.

## 1. 환경 설정

### 가상 환경 구성

가상 환경 생성 및 활성화 방법은 [`venv_guide.md`](./venv_guide.md)를 참고하세요.

### 의존성 설치

가상 환경 활성화 후 패키지를 설치합니다.

```bash
pip install -r requirements.txt
```

### 환경 변수 설정

`.env.example`을 복사하여 `.env` 파일을 생성하고, API 키를 입력합니다.

```bash
cp .env.example .env
```

`.env` 파일에서 `GMS_API_KEY`를 실제 값으로 수정하세요.

```dotenv
GMS_API_KEY=실제_API_키
GMS_BASE_URL=https://gms.ssafy.io/gmsapi/api.openai.com/v1
LLM_MODEL=gpt-4o-mini
DEBUG=false
ALLOWED_ORIGINS=["http://localhost:3000"]
```

## 2. 서버 실행

```bash
uvicorn app.main:app --reload --port 8000
```

서버 구동 후 `http://localhost:8000/docs`에서 Swagger UI로 API를 확인할 수 있습니다.

## 3. API 사용법

### 채팅 요청

```bash
curl -X POST http://localhost:8000/api/v1/chat/ \
  -H "Content-Type: application/json" \
  -d '{"message": "종합소득세가 뭐야?"}'
```

### 세션 유지 채팅

응답에서 받은 `session_id`를 포함하여 대화를 이어갑니다.

```bash
curl -X POST http://localhost:8000/api/v1/chat/ \
  -H "Content-Type: application/json" \
  -d '{"message": "더 자세히 알려줘", "session_id": "응답에서_받은_session_id"}'
```

### 대화 히스토리 조회

```bash
curl http://localhost:8000/api/v1/chat/history/{session_id}
```

## 4. 테스트 실행

```bash
python -m pytest tests/ -v
```

## 5. 프로젝트 구조

```
ai/
├── app/
│   ├── core/           # 설정, 예외, 프롬프트
│   ├── models/         # Pydantic 모델
│   ├── routers/        # API 라우터
│   └── services/       # 비즈니스 로직 (ChatService, RetrievalService)
├── tests/              # 테스트
├── .env.example        # 환경 변수 예시
├── requirements.txt    # 의존성
└── venv_guide.md       # 가상 환경 가이드
```