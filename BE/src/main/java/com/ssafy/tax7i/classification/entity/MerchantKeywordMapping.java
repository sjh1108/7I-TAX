package com.ssafy.tax7i.classification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "merchant_keyword_mapping", indexes = {
        @Index(name = "idx_mkm_keyword", columnList = "keyword"),
        @Index(name = "idx_mkm_tax_category", columnList = "taxCategory")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantKeywordMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false, length = 10)
    private String mcc = "5817";

    @Column(nullable = false, length = 50)
    private String taxCategory;

    @Column(nullable = false)
    private Boolean isDomestic = true;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
