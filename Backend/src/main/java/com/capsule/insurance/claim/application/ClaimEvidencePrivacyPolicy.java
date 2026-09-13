package com.capsule.insurance.claim.application;

import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.claim.dto.ClaimEvidenceResponse;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ClaimEvidencePrivacyPolicy {

    private static final Set<String> ALLOWED_METADATA_KEYS = Set.of(
            "documentType",
            "issuedDate",
            "issuerCategory",
            "pageCount",
            "fixture"
    );
    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of(
            "name",
            "phone",
            "email",
            "address",
            "resident",
            "ssn",
            "registration",
            "account",
            "diagnosisdetail",
            "medicalrecord",
            "note"
    );
    private static final String CLASSIFICATION_PATTERN = "[A-Z][A-Z0-9_]{0,49}";

    public Map<String, Object> validateAndCopyMetadata(Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            validateKey(key);
            validateValue(key, value);
            safeMetadata.put(key, value);
        });
        return Map.copyOf(safeMetadata);
    }

    public ClaimEvidenceResponse toResponse(ClaimEvidence evidence) {
        return new ClaimEvidenceResponse(
                evidence.claimEvidenceId(),
                evidence.claimId(),
                evidence.evidenceType(),
                "synthetic://redacted",
                evidence.checksum(),
                safeResponseMetadata(evidence.metadata()),
                evidence.verified(),
                evidence.createdAt()
        );
    }

    private Map<String, Object> safeResponseMetadata(Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && ALLOWED_METADATA_KEYS.contains(key) && isValidValue(key, value)) {
                safeMetadata.put(key, value);
            }
        });
        return Map.copyOf(safeMetadata);
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw invalidMetadata("증빙 메타데이터 키는 비어 있을 수 없습니다.");
        }
        String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        if (SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(normalized::contains)) {
            throw invalidMetadata("민감정보 필드는 청구 증빙 메타데이터에 저장할 수 없습니다: " + key);
        }
        if (!ALLOWED_METADATA_KEYS.contains(key)) {
            throw invalidMetadata("허용되지 않은 청구 증빙 메타데이터입니다: " + key);
        }
    }

    private void validateValue(String key, Object value) {
        if (!isValidValue(key, value)) {
            throw invalidMetadata("청구 증빙 메타데이터 값 형식이 올바르지 않습니다: " + key);
        }
    }

    private boolean isValidValue(String key, Object value) {
        return switch (key) {
            case "documentType", "issuerCategory" ->
                    value instanceof String text && text.matches(CLASSIFICATION_PATTERN);
            case "issuedDate" -> value instanceof String text && isIsoDate(text);
            case "pageCount" -> value instanceof Integer count && count >= 1 && count <= 100;
            case "fixture" -> value instanceof Boolean;
            default -> false;
        };
    }

    private boolean isIsoDate(String value) {
        try {
            return LocalDate.parse(value).toString().equals(value);
        } catch (DateTimeException exception) {
            return false;
        }
    }

    private BusinessException invalidMetadata(String message) {
        return new BusinessException(ErrorCode.INVALID_INPUT, message);
    }
}
