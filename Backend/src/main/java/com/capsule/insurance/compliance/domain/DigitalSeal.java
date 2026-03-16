// #Demo Setting
package com.capsule.insurance.compliance.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DigitalSeal {

    private final Long sealId;
    private final Long userId;
    private final String providerName;
    private final DigitalSealStatus sealStatus;
    private final String encryptedToken;
    private final Integer keyVersion;
    private final LocalDateTime issuedAt;
    private final LocalDateTime lastVerifiedAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime revokedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
