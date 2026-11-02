package com.capsule.insurance.claim.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.common.exception.BusinessException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClaimEvidencePrivacyPolicyTest {

    private final ClaimEvidencePrivacyPolicy policy = new ClaimEvidencePrivacyPolicy();

    @Test
    void rejectsSensitiveAndUnknownMetadataKeys() {
        assertThatThrownBy(() -> policy.validateAndCopyMetadata(Map.of("patient_name", "홍길동")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("민감정보");
        assertThatThrownBy(() -> policy.validateAndCopyMetadata(Map.of("rawPayload", "value")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("허용되지 않은");
    }

    @Test
    void rejectsFreeTextHiddenUnderAllowedMetadataKeys() {
        assertThatThrownBy(() -> policy.validateAndCopyMetadata(Map.of(
                "documentType", "환자 홍길동의 진단서"
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("값 형식");
        assertThatThrownBy(() -> policy.validateAndCopyMetadata(Map.of("pageCount", 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("값 형식");
        assertThatThrownBy(() -> policy.validateAndCopyMetadata(Map.of("issuedDate", "2026/09/13")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("값 형식");
    }

    @Test
    void masksStorageReferenceWithoutChangingSafeMetadata() {
        Map<String, Object> metadata = Map.of(
                "documentType", "DIAGNOSIS_CERTIFICATE",
                "pageCount", 1,
                "patientName", "과거 저장 데이터"
        );
        ClaimEvidence evidence = new ClaimEvidence(
                1L,
                2L,
                "DEMO_DIAGNOSIS_CERTIFICATE",
                "synthetic://bucket/private-key",
                "a".repeat(64),
                metadata,
                true,
                Instant.parse("2026-09-13T10:00:00Z")
        );

        var response = policy.toResponse(evidence);

        assertThat(response.syntheticReference()).isEqualTo("synthetic://redacted");
        assertThat(response.metadata()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "documentType", "DIAGNOSIS_CERTIFICATE",
                "pageCount", 1
        ));
        assertThat(response.metadata()).doesNotContainKey("patientName");
    }
}
