package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OnboardingCategoryRequest(
        @NotEmpty(message = "categoryCodes는 최소 1개 이상이어야 합니다.")
        List<String> categoryCodes
) {
}
