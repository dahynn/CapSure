package com.capsule.insurance.application.dto;

import jakarta.validation.constraints.NotNull;

public record ReplaceDisclosuresRequest(
        @NotNull Boolean diagnosedCancer,
        @NotNull Boolean underCancerExamination,
        @NotNull Boolean recentHospitalization
) {
}
