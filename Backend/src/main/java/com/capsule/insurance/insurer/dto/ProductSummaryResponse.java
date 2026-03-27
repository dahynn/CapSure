package com.capsule.insurance.insurer.dto;

import com.capsule.insurance.insurer.domain.CoverageCategory;
import com.capsule.insurance.insurer.domain.InsurerSector;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 프로젝트 전체에서 통합하여 사용하는 보험 상품 요약 DTO 레코드입니다.
 * MyBatis 조회 및 API 응답에 모두 사용됩니다.
 */
public record ProductSummaryResponse(
        Long productSourceId,
        String companyName,
        String productName,
        InsurerSector insurerSector,
        String coverageCategoryCode,
        String coverageCode,
        BigDecimal monthlyPrice,
        Instant loadedAt,
        Instant updatedAt
) {
    /**
     * API 호환성을 위한 카테고리 Enum 반환 메소드
     */
    public CoverageCategory category() {
        return coverageCategoryCode != null ? CoverageCategory.valueOf(coverageCategoryCode) : null;
    }
}

