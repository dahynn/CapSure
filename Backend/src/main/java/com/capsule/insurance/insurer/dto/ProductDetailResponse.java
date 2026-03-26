package com.capsule.insurance.insurer.dto;

import com.capsule.insurance.insurer.domain.InsurerSector;
import java.math.BigDecimal;
import java.time.LocalDate;

// 보험 상품 상세 정보를 담는 DTO 레코드입니다.

public record ProductDetailResponse(
                Long productSourceId, // 상품 소스 ID
                String companyName, // 보험사명 (예: 삼성생명, 현대해상)
                String productName, // 상품명
                InsurerSector insurerSector, // 업권 구분 (LIFE: 생명보험, NONLIFE: 손해보험)
                String saleChannel, // 판매 채널 (방카슈랑스, 대면 등)
                String coverageName, // 담보명/급부명 (예: 암진단비, 질병입원일당)
                String claimReason, // 보험금 지급 사유
                String payoutAmount, // 지급 금액 상세 설명
                String joinAmount, // 가입 금액 (한도)
                String minimumJoinPremium, // 최소 가입 보험료
                String paymentCycle, // 납입 주기 (월납, 연납 등)
                String paymentTerm, // 납입 기간 (10년납, 20년납 등)
                String coverageTerm, // 보장 기간 (80세 만기, 100세 만기 등)
                BigDecimal monthlyPrice, // 월 납입 보험료 (조회 시 성별에 맞춰 계산된 값)
                String productSummary, // 상품 요약 설명
                String productFeature, // 상품의 주요 특징 (특장점)
                String specialNote, // 기타 특이사항
                String contactPhone, // 보험사 연락처
                LocalDate saleDate, // 판매 시작일
                String currentAnnouncedRate, // 공시 이율
                String fixedRate, // 확정 이율
                String minimumGuaranteedRate // 최저 보증 이율
) {
}
