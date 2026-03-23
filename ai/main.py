from fastapi import FastAPI

# 1. FastAPI 애플리케이션 인스턴스 생성
app = FastAPI()

# 2. 엔드포인트(라우터) 정의

@app.get("/")
def read_root():
    """
    기본 루트 경로('/')로 GET 요청이 오면 환영 메시지를 반환합니다.
    """
    return {"message": "Hello, FastAPI Environment!"}

@app.get("/items/{item_id}")
def read_item(item_id: int, q: str | None = None):
    """
    경로 매개변수와 쿼리 매개변수를 처리하는 예시입니다.
    예: /items/5?q=somequery
    """
    return {"item_id": item_id, "q": q}

# 3. 서버 실행 방법 (터미널에서 명령어 입력)
# uvicorn main:app --reload
