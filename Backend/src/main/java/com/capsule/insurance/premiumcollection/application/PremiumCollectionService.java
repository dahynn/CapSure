package com.capsule.insurance.premiumcollection.application;

import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import com.capsule.insurance.premiumcollection.dto.CreatePremiumReceivableRequest;
import com.capsule.insurance.premiumcollection.dto.InstantSettlementRequest;
import com.capsule.insurance.premiumcollection.infra.JdbcPremiumCollectionRepository;
import com.capsule.insurance.premiumcollection.dto.PremiumCollectionTimelineResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PremiumCollectionService {
    private final JdbcPremiumCollectionRepository repository;
    private final PremiumDelinquencyService delinquency;

    public PremiumCollectionService(JdbcPremiumCollectionRepository repository, PremiumDelinquencyService delinquency) {
        this.repository = repository;
        this.delinquency = delinquency;
    }

    @Transactional
    public PremiumCollectionSnapshot createDue(CreatePremiumReceivableRequest request) {
        return repository.createDueWithAutomaticInstruction(request, "COL-" + UUID.randomUUID());
    }

    @Transactional
    public PremiumCollectionSnapshot settleImmediately(InstantSettlementRequest request) {
        delinquency.beforeSettlement(repository.policyIdForReceivable(request.premiumReceivableId()));
        PremiumCollectionSnapshot result = repository.settleImmediately(request, "COL-" + UUID.randomUUID());
        delinquency.afterSettlement(result.policyId());
        return result;
    }

    @Transactional
    public PremiumCollectionSnapshot captureAutomaticDebit(Long instructionId, String providerTransactionKey) {
        delinquency.beforeSettlement(repository.policyIdForInstruction(instructionId));
        PremiumCollectionSnapshot result = repository.captureAutomaticDebit(instructionId, providerTransactionKey);
        delinquency.afterSettlement(result.policyId());
        return result;
    }

    @Transactional
    public int createDuplicateDebitRefundCases() {
        return repository.createDuplicateDebitRefundCases();
    }

    public PremiumCollectionTimelineResponse getTimeline(int limit) {
        return repository.loadTimeline(Math.min(Math.max(limit, 1), 50));
    }
}
