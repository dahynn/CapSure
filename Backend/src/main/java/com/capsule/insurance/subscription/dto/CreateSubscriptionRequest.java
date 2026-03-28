package com.capsule.insurance.subscription.dto;

import java.util.List;

/**
 * 캡슐 보험 최초 가입 신청 시 사용하는 DTO 입니다.
 */
public record CreateSubscriptionRequest(
    List<Long> productSourceIds,
    String capsuleName
) {}
