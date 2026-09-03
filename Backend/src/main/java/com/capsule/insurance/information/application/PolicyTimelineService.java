package com.capsule.insurance.information.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.information.application.port.FinancialEventAuditRepository;
import com.capsule.insurance.information.domain.FinancialEventAudit;
import com.capsule.insurance.information.dto.PolicyTimelineEventResponse;
import com.capsule.insurance.policy.application.port.PolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PolicyTimelineService {

    private final PolicyRepository policyRepository;
    private final FinancialEventAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public PolicyTimelineService(
            PolicyRepository policyRepository,
            FinancialEventAuditRepository auditRepository,
            ObjectMapper objectMapper
    ) {
        this.policyRepository = policyRepository;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public List<PolicyTimelineEventResponse> get(Long userId, Long policyId) {
        policyRepository.findOwned(policyId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "보험계약을 찾을 수 없습니다."
                ));
        return auditRepository.findByPolicyId(policyId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PolicyTimelineEventResponse toResponse(FinancialEventAudit event) {
        try {
            return new PolicyTimelineEventResponse(
                    event.eventId(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.eventType(),
                    event.policyId(),
                    objectMapper.readTree(event.payloadJson()),
                    event.payloadHash(),
                    event.occurredAt(),
                    event.projectedAt()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("감사 타임라인 payload를 읽을 수 없습니다.", exception);
        }
    }
}
