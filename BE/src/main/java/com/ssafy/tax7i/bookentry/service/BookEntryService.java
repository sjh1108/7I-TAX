package com.ssafy.tax7i.bookentry.service;

import com.ssafy.tax7i.bookentry.dto.BookEntryCategoryUpdateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookEntryService {

    private final BookEntryRepository bookEntryRepository;

    @CacheEvict(value = "entertainmentUsed", key = "#userId")
    @Transactional
    public BookEntryResponse create(Long userId, BookEntryCreateRequest request) {
        // 중복 장부 방어: 같은 paymentId로 이미 생성된 경우
        if (request.paymentId() != null) {
            bookEntryRepository.findByPaymentId(request.paymentId()).ifPresent(existing -> {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOK_ENTRY,
                        "이미 장부가 생성된 결제입니다. paymentId=" + request.paymentId());
            });
        }

        long amount = request.amount();
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "금액은 0보다 커야 합니다.");
        }

        long vatAmount;
        long supplyPrice;
        if (request.isVatExemptOrDefault()) {
            vatAmount = 0L;
            supplyPrice = amount;
        } else {
            vatAmount = Math.round(amount / 11.0);
            supplyPrice = amount - vatAmount;
        }

        long incomeAmount = 0L;
        long expenseAmount = 0L;
        long fixedAssetAmount = 0L;

        switch (request.entryType()) {
            case INCOME -> incomeAmount = supplyPrice;
            case EXPENSE -> expenseAmount = supplyPrice;
            case ASSET -> fixedAssetAmount = supplyPrice;
        }

        BookEntry entry = BookEntry.builder()
                .userId(userId)
                .paymentId(request.paymentId())
                .entryDate(request.entryDate())
                .description(request.description())
                .merchantName(request.merchantName())
                .entryType(request.entryType())
                .incomeAmount(incomeAmount)
                .expenseAmount(expenseAmount)
                .fixedAssetAmount(fixedAssetAmount)
                .vatAmount(vatAmount)
                .supplyPrice(supplyPrice)
                .categoryCode(request.categoryCode())
                .categoryName(request.categoryName())
                .isVatDeductible(!request.isVatExemptOrDefault())
                .note(request.note())
                .build();

        BookEntry saved = bookEntryRepository.save(entry);
        log.info("장부 생성: id={}, userId={}, paymentId={}, type={}, amount={}, supply={}, vat={}, exempt={}",
                saved.getId(), userId, request.paymentId(), request.entryType(),
                amount, supplyPrice, vatAmount, request.isVatExemptOrDefault());
        return BookEntryResponse.from(saved);
    }

    public BookEntryResponse getEntry(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        return BookEntryResponse.from(entry);
    }

    public Page<BookEntryResponse> getEntries(Long userId, Boolean confirmed, Pageable pageable) {
        Page<BookEntry> page;
        if (confirmed != null) {
            page = bookEntryRepository.findByUserIdAndConfirmed(userId, confirmed, pageable);
        } else {
            page = bookEntryRepository.findByUserId(userId, pageable);
        }
        return page.map(BookEntryResponse::from);
    }

    public long getUnconfirmedCount(Long userId) {
        return bookEntryRepository.countByUserIdAndConfirmed(userId, false);
    }

    @Transactional
    public BookEntryResponse confirm(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.confirm();
        log.info("장부 확인: id={}, userId={}, merchant={}", entryId, userId, entry.getMerchantName());
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse updateCategory(Long userId, Long entryId,
                                            BookEntryCategoryUpdateRequest request) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        String beforeCode = entry.getCategoryCode();
        entry.updateCategory(request.categoryCode(), request.categoryName());
        log.info("세목 변경: id={}, userId={}, {} → {}", entryId, userId, beforeCode, request.categoryCode());
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse markAsPersonal(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.markAsPersonal();
        log.info("개인지출 전환: id={}, userId={}, merchant={}, amount={}",
                entryId, userId, entry.getMerchantName(), entry.getExpenseAmount());
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse markAsBusiness(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.markAsBusiness();
        log.info("사업경비 복원: id={}, userId={}", entryId, userId);
        return BookEntryResponse.from(entry);
    }

    private BookEntry findByIdAndUserId(Long entryId, Long userId) {
        return bookEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> {
                    log.warn("장부 조회 실패: entryId={}, userId={}", entryId, userId);
                    return new BusinessException(ErrorCode.BOOK_ENTRY_NOT_FOUND);
                });
    }
}
