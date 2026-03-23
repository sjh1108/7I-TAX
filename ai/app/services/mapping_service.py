import json
import logging
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger(__name__)


@dataclass
class MccInfo:
    mcc: str
    category: str
    expense_type: str
    deductible: bool | str
    industry_code: str


@dataclass
class ExpenseRate:
    industry_code: str
    industry_name: str
    simple_rate: float
    standard_rate: float


class MappingService:
    """MCC 코드와 업종별 경비율 매핑 서비스.

    mappings.json과 expense_rates.json을 로드하여
    카드 결제 MCC 코드 기반 경비 분류를 지원한다.
    """

    def __init__(self, data_dir: str = "app/data/intents") -> None:
        self.data_dir = Path(data_dir)
        self._mcc_map: dict[str, MccInfo] = {}
        self._expense_rates: dict[str, ExpenseRate] = {}

    def load(self) -> None:
        """데이터 파일을 로드하여 내부 맵을 구축한다.

        mappings.json -> _mcc_map
        expense_rates.json -> _expense_rates

        파일이 없거나 파싱 오류 시 logging.warning 후 빈 dict 유지.
        """
        self._load_mappings()
        self._load_expense_rates()

    def _load_mappings(self) -> None:
        """mappings.json을 로드하여 _mcc_map과 _expense_rates를 구축한다."""
        mappings_path = self.data_dir / "mappings.json"
        if not mappings_path.exists():
            logger.warning("mappings.json 파일을 찾을 수 없습니다: %s", mappings_path)
            return

        try:
            with open(mappings_path, encoding="utf-8") as f:
                data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            logger.warning("mappings.json 로드 실패: %s", e)
            return

        mcc_to_category: dict = data.get("mcc_to_category", {})
        for mcc, info in mcc_to_category.items():
            self._mcc_map[mcc] = MccInfo(
                mcc=mcc,
                category=info.get("category", "미분류"),
                expense_type=info.get("expense_type", "미분류"),
                deductible=info.get("deductible", False),
                industry_code=info.get("industry_code", ""),
            )

        # expense_rate_lookup도 _expense_rates에 병합 (보조 데이터)
        expense_rate_lookup: dict = data.get("expense_rate_lookup", {})
        for code, rate_info in expense_rate_lookup.items():
            if code not in self._expense_rates:
                self._expense_rates[code] = ExpenseRate(
                    industry_code=code,
                    industry_name=rate_info.get("industry", ""),
                    simple_rate=float(rate_info.get("simple_rate", 0.0)),
                    standard_rate=float(rate_info.get("standard_rate", 0.0)),
                )

    def _load_expense_rates(self) -> None:
        """expense_rates.json을 로드하여 _expense_rates를 구축한다.

        이미 mappings.json의 expense_rate_lookup에서 로드된 항목도
        expense_rates.json의 데이터로 덮어쓴다 (더 상세한 데이터 우선).
        """
        rates_path = self.data_dir / "expense_rates.json"
        if not rates_path.exists():
            logger.warning("expense_rates.json 파일을 찾을 수 없습니다: %s", rates_path)
            return

        try:
            with open(rates_path, encoding="utf-8") as f:
                data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            logger.warning("expense_rates.json 로드 실패: %s", e)
            return

        rates: list[dict] = data.get("rates", [])
        for entry in rates:
            code = entry.get("industry_code", "")
            if not code:
                continue
            self._expense_rates[code] = ExpenseRate(
                industry_code=code,
                industry_name=entry.get("industry_name", ""),
                simple_rate=float(entry.get("simple_expense_rate", 0.0)),
                standard_rate=float(entry.get("standard_expense_rate", 0.0)),
            )

    def lookup_mcc(self, mcc: str) -> MccInfo | None:
        """MCC 코드로 MccInfo를 조회한다."""
        return self._mcc_map.get(mcc)

    def lookup_expense_rate(self, industry_code: str) -> ExpenseRate | None:
        """업종 코드로 ExpenseRate를 조회한다."""
        return self._expense_rates.get(industry_code)

    def classify_transaction(self, mcc: str) -> dict:
        """MCC 코드로 거래를 분류하고 경비율을 포함한 결과를 반환한다.

        처리 순서:
        1. MCC -> MccInfo 조회
        2. MccInfo.industry_code -> ExpenseRate 조회
        3. 결과 딕셔너리 반환

        Args:
            mcc: MCC(Merchant Category Code) 문자열

        Returns:
            {
                "mcc": str,
                "category": str,
                "expense_type": str,
                "deductible": bool | str,
                "industry_name": str | None,
                "simple_rate": float | None,
                "standard_rate": float | None,
            }
        """
        mcc_info = self.lookup_mcc(mcc)
        if mcc_info is None:
            return {
                "mcc": mcc,
                "category": "미분류",
                "expense_type": "미분류",
                "deductible": False,
                "industry_name": None,
                "simple_rate": None,
                "standard_rate": None,
            }

        expense_rate = self.lookup_expense_rate(mcc_info.industry_code)

        return {
            "mcc": mcc,
            "category": mcc_info.category,
            "expense_type": mcc_info.expense_type,
            "deductible": mcc_info.deductible,
            "industry_name": expense_rate.industry_name if expense_rate else None,
            "simple_rate": expense_rate.simple_rate if expense_rate else None,
            "standard_rate": expense_rate.standard_rate if expense_rate else None,
        }
