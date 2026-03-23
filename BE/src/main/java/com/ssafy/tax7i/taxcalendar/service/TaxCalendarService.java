package com.ssafy.tax7i.taxcalendar.service;

import com.ssafy.tax7i.taxcalendar.dto.TaxDeadlineResponse;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TaxCalendarService {

    private record TaxSchedule(String name, String description, int month, int day) {}

    private static final List<TaxSchedule> SCHEDULES = List.of(
            new TaxSchedule("부가가치세 확정신고 (2기)", "7~12월 매출·매입 부가세 확정신고 및 납부", 1, 25),
            new TaxSchedule("부가가치세 예정신고 (1기)", "1~3월 매출·매입 부가세 예정신고 및 납부", 4, 25),
            new TaxSchedule("종합소득세 신고", "전년도 사업소득 종합소득세 확정신고 및 납부", 5, 31),
            new TaxSchedule("지방소득세 신고", "종합소득세 기준 지방소득세 신고 및 납부", 5, 31),
            new TaxSchedule("부가가치세 확정신고 (1기)", "1~6월 매출·매입 부가세 확정신고 및 납부", 7, 25),
            new TaxSchedule("부가가치세 예정신고 (2기)", "7~9월 매출·매입 부가세 예정신고 및 납부", 10, 25)
    );

    @Cacheable(value = "taxDeadlines", key = "#today")
    public List<TaxDeadlineResponse> getUpcomingDeadlines(LocalDate today) {
        List<TaxDeadlineResponse> result = new ArrayList<>();
        int year = today.getYear();

        for (TaxSchedule s : SCHEDULES) {
            LocalDate deadline = LocalDate.of(year, s.month(), s.day());
            if (deadline.isBefore(today)) {
                deadline = deadline.plusYears(1);
            }
            result.add(toResponse(s, deadline, today));
        }

        result.sort(Comparator.comparingLong(TaxDeadlineResponse::dDay));
        return result;
    }

    private TaxDeadlineResponse toResponse(TaxSchedule s, LocalDate deadline, LocalDate today) {
        long dDay = ChronoUnit.DAYS.between(today, deadline);
        return new TaxDeadlineResponse(s.name(), s.description(), deadline.toString(), dDay);
    }
}
