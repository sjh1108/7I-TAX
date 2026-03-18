package com.ssafy.tax7i.taxcalendar.dto;

import java.time.LocalDate;

public record TaxDeadlineResponse(
        String taxName,
        String description,
        LocalDate deadline,
        long dDay
) {
}
