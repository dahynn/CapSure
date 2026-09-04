package com.capsule.insurance.premiumcollection.dto;

import jakarta.validation.constraints.NotBlank;

public record CaptureCollectionRequest(@NotBlank String providerTransactionKey) {
}
