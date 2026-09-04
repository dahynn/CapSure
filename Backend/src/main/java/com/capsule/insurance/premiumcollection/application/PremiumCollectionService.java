package com.capsule.insurance.premiumcollection.application;

import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import com.capsule.insurance.premiumcollection.dto.CreatePremiumReceivableRequest;
import com.capsule.insurance.premiumcollection.dto.InstantSettlementRequest;
import com.capsule.insurance.premiumcollection.infra.JdbcPremiumCollectionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PremiumCollectionService {
    private final JdbcPremiumCollectionRepository repository;

    public PremiumCollectionService(JdbcPremiumCollectionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PremiumCollectionSnapshot createDue(CreatePremiumReceivableRequest request) {
        return repository.createDueWithAutomaticInstruction(request, "COL-" + UUID.randomUUID());
    }

    @Transactional
    public PremiumCollectionSnapshot settleImmediately(InstantSettlementRequest request) {
        return repository.settleImmediately(request, "COL-" + UUID.randomUUID());
    }

    @Transactional
    public PremiumCollectionSnapshot captureAutomaticDebit(Long instructionId, String providerTransactionKey) {
        return repository.captureAutomaticDebit(instructionId, providerTransactionKey);
    }

    @Transactional
    public int createDuplicateDebitRefundCases() {
        return repository.createDuplicateDebitRefundCases();
    }
}
