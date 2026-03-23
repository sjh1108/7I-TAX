package com.ssafy.tax7i.tax.dto;

import java.util.List;

public record TaxSavingResponse(
        long currentFinalTax,
        List<TaxSavingRecommendation> recommendations,
        long potentialTotalSaving
) {
}
