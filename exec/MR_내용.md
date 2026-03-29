# MR 제목

```
docs: exec 포팅 매뉴얼 작성 (release 브랜치 기반)
```

# MR 내용

## Summary
산출물5 포팅 매뉴얼을 `exec/` 폴더에 작성하였습니다.
release 브랜치 기준으로 실제 배포 환경과 일치하도록 작성했습니다.

## 포함 내용

### 1. 빌드 및 배포 문서
- **제품/버전 정보**: JVM 17, Spring Boot 3.5.11, Nginx Alpine, Python 3.11, PostgreSQL 16, Redis 7
- **BE 주요 라이브러리**: jjwt 0.12.6, Apache POI 5.2.5, openhtmltopdf 1.0.10, firebase-admin 9.4.3, solapi-sdk 1.1.0
- **AI 주요 라이브러리**: LangChain, ChromaDB, PyTorch (CPU), Transformers
- **Android**: compileSdk 36, Kotlin 2.2.10, Compose BOM 2026.02.01, Hilt 2.57.1, CameraX 1.4.2
- **환경변수**: Backend 21개, AI 서비스 15개, Android 빌드 설정
- **로컬 개발 환경**: `docker-compose.yml` (PostgreSQL, Redis, AI, Worker) + 헬스체크
- **운영 배포**: Jenkinsfile Pipeline (5단계), Docker 포트 매핑, 볼륨 마운트
- **DB 접속정보**: 로컬(`tax7i`/`ssafy`) / 운영(환경변수) 분리 기재
- **Spring 설정**: Async/Retry/Scheduling, Redis 캐시 6종, Security 공개 엔드포인트
- **템플릿**: Excel 2개(income_tax, vat), PDF 3개(simple-ledger, tax-receipt, tax-return)

### 2. 외부 서비스 정보
- SSAFY 금융망 API (계좌/카드/이체)
- GMS OpenAI 프록시 (AI 챗봇/거래 분류)
- Solapi SMS (OTP 인증)
- Firebase Cloud Messaging (푸시 알림)

### 3. DB 덤프
- 초기화 SQL 5개 (`db/init/01~05`)
- 스키마 22테이블, 시드 데이터, 2025 세금 기준, 테스트 결제내역, 금액 보정
- docker-compose 최초 기동 시 자동 실행

### 4. 시연 시나리오 (7개)
- 회원가입 및 로그인
- 카드 등록 (SMS 인증 포함)
- Pay 등록 및 QR 결제
- 자동 장부 분류 및 조회
- 세금 신고 및 절세
- AI 챗봇 상담
- 데이터 내보내기

## 변경 파일
- `exec/포팅_매뉴얼.md` (신규)
