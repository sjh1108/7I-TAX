package com.ssafy.tax7i.bookentry.dto;

import java.util.List;

public record BookEntrySummaryResponse(
        int year,
        long totalIncome,
        long totalExpense,
        List<MonthSummary> byMonth,
        List<CategorySummary> byCategory
) {
    public record MonthSummary(int month, long income, long expense) {}

    public record CategorySummary(String code, String name, long amount, long count) {}
}
