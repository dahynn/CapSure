// #Demo Setting
package com.capsule.insurance.compliance.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DigitalSeal {

    private Long sealId;
    private Long userId;
    private String providerName;
    private DigitalSealStatus sealStatus;
    private String encryptedToken;
    private Integer keyVersion;
    private LocalDateTime issuedAt;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TODO: 이 파트에서 디지털 인감 과거 이력 보존 방식이라는 논의점이 있는데, 우선 REGISTERED 상태이면서 revokedAt이 없는 row를 유효 인감으로 해석했음.
    //       프로젝트 범위상 디지털 인감을 생성해두면 변경을 못한다던지 정해야 함
    public boolean isValidSeal() {
        return sealStatus == DigitalSealStatus.REGISTERED && revokedAt == null;
    }
}
