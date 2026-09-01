package com.capsule.insurance.policy.application.port;

import com.capsule.insurance.policy.domain.InsurancePolicy;
import java.util.Optional;

public interface PolicyRepository {

    InsurancePolicy createPending(
            String policyNo,
            Long applicationId,
            Long policyholderUserId,
            Long insuredUserId,
            Long beneficiaryUserId
    );

    InsurancePolicy activateFromPaidOrder(Long paymentOrderId);

    Optional<InsurancePolicy> findOwned(Long policyId, Long userId);

    Optional<InsurancePolicy> findById(Long policyId);
}
