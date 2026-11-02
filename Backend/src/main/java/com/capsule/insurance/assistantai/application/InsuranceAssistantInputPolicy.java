package com.capsule.insurance.assistantai.application;

import com.capsule.insurance.assistantai.domain.InsuranceAssistantEvidence;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantRequest;
import com.capsule.insurance.assistantai.domain.InsuranceEvidenceType;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class InsuranceAssistantInputPolicy {

    private static final Set<InsuranceEvidenceType> ALLOWED_EVIDENCE_TYPES =
            EnumSet.of(InsuranceEvidenceType.TERMS_DOCUMENT, InsuranceEvidenceType.PRODUCT_SOURCE);
    private static final Pattern RESIDENT_REGISTRATION_NUMBER = Pattern.compile("\\b\\d{6}-?[1-4]\\d{6}\\b");
    private static final Pattern BANK_ACCOUNT_NUMBER = Pattern.compile("\\b\\d{2,6}-\\d{2,6}-\\d{2,8}\\b");
    private static final Pattern PHONE_NUMBER = Pattern.compile("\\b01[0-9]-?\\d{3,4}-?\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");

    public PolicyEvaluation evaluate(InsuranceAssistantRequest request) {
        for (InsuranceAssistantEvidence evidence : request.evidence()) {
            if (!ALLOWED_EVIDENCE_TYPES.contains(evidence.type())) {
                return PolicyEvaluation.UNAPPROVED_EVIDENCE;
            }
        }

        if (containsSensitiveValue(request.instruction())) {
            return PolicyEvaluation.SENSITIVE_INPUT;
        }
        return request.evidence().stream().anyMatch(evidence -> containsSensitiveValue(evidence.content()))
                ? PolicyEvaluation.SENSITIVE_INPUT
                : PolicyEvaluation.ALLOWED;
    }

    private boolean containsSensitiveValue(String value) {
        return RESIDENT_REGISTRATION_NUMBER.matcher(value).find()
                || BANK_ACCOUNT_NUMBER.matcher(value).find()
                || PHONE_NUMBER.matcher(value).find()
                || EMAIL.matcher(value).find();
    }

    public enum PolicyEvaluation {
        ALLOWED,
        UNAPPROVED_EVIDENCE,
        SENSITIVE_INPUT
    }
}
