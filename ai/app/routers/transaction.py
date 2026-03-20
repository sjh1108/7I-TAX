import logging

from fastapi import APIRouter, Depends

from app.core.dependencies import get_tax_classifier_service
from app.models.transaction import (
    TransactionClassifyRequest,
    TransactionClassifyResponse,
)
from app.services.tax_classifier_service import TaxClassifierService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/transaction", tags=["transaction"])


@router.post("/classify", response_model=TransactionClassifyResponse)
async def classify_transaction(
    request: TransactionClassifyRequest,
    service: TaxClassifierService = Depends(get_tax_classifier_service),
) -> TransactionClassifyResponse:
    """거래 내역을 세목으로 분류한다. (로컬 fine-tuned 모델)"""
    result = service.classify(request.description)
    return TransactionClassifyResponse(
        category=result["category"],
        confidence=result["confidence"],
        method=result["method"],
    )
