import logging
from dataclasses import dataclass

import httpx

from app.core.config import Settings

logger = logging.getLogger(__name__)


@dataclass
class Transaction:
    """거래 내역 (아키텍처 설계 문서 7.2절)."""

    date: str
    amount: int
    merchant_name: str
    merchant_code: str
    mcc: str
    category: str | None = None


@dataclass
class BusinessInfo:
    """사업자 정보 (아키텍처 설계 문서 7.2절)."""

    business_type: str       # 개인사업자 | 법인
    industry_code: str       # 업종코드
    tax_type: str            # 일반과세 | 간이과세
    establishment_date: str  # 사업 개시일


class BackendClient:
    """백엔드(뱅크앱) API 클라이언트.

    아키텍처 설계 문서 7.2절 스펙.
    httpx 비동기 클라이언트 사용.
    """

    def __init__(self, settings: Settings) -> None:
        self.client = httpx.AsyncClient(
            base_url=settings.backend_base_url,
            headers={"Authorization": f"Bearer {settings.backend_api_key}"},
            timeout=10.0,
        )

    async def get_transactions(
        self,
        user_id: str,
        period: str | None = None,
        limit: int = 100,
    ) -> list[Transaction]:
        """사용자 거래 내역 조회.

        API: GET /api/v1/users/{user_id}/transactions
        파라미터: period (YYYY-MM), limit
        반환: list[Transaction]

        에러 처리:
        - 404: 빈 리스트 반환
        - 500/timeout: 로그 남기고 빈 리스트 반환 (파이프라인 중단 방지)
        """
        try:
            resp = await self.client.get(
                f"/api/v1/users/{user_id}/transactions",
                params={"period": period, "limit": limit},
            )
            resp.raise_for_status()
            return [Transaction(**t) for t in resp.json()["transactions"]]
        except (httpx.HTTPStatusError, httpx.TimeoutException, Exception) as e:
            logger.warning("거래 내역 조회 실패: %s", e)
            return []

    async def get_business_info(self, user_id: str) -> BusinessInfo | None:
        """사업자 정보 조회.

        API: GET /api/v1/users/{user_id}/business-info
        반환: BusinessInfo | None

        에러 처리: 실패 시 None 반환
        """
        try:
            resp = await self.client.get(f"/api/v1/users/{user_id}/business-info")
            resp.raise_for_status()
            return BusinessInfo(**resp.json())
        except Exception as e:
            logger.warning("사업자 정보 조회 실패: %s", e)
            return None

    async def close(self) -> None:
        """HTTP 클라이언트 종료."""
        await self.client.aclose()
