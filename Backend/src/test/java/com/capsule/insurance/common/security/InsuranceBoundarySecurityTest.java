package com.capsule.insurance.common.security;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.claim.api.ClaimController;
import com.capsule.insurance.claim.application.ClaimService;
import com.capsule.insurance.common.security.jwt.*;
import com.capsule.insurance.policy.api.PolicyController;
import com.capsule.insurance.policy.application.PolicyService;
import com.capsule.insurance.operations.reconciliation.api.PaymentReconciliationOperationsController;
import com.capsule.insurance.operations.reconciliation.application.PaymentReconciliationBatchService;
import com.capsule.insurance.operations.recovery.application.OperationsRecoveryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PolicyController.class, ClaimController.class, PaymentReconciliationOperationsController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class InsuranceBoundarySecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean PolicyService policies;
    @MockitoBean ClaimService claims;
    @MockitoBean PaymentReconciliationBatchService reconciliation;
    @MockitoBean OperationsRecoveryService recovery;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenBlacklistRepository blacklist;

    @Test void anonymousCannotReadOrMutateInsuranceOrRunOperations() throws Exception {
        for (String path : List.of("/api/v1/policies/1", "/api/v1/claims/1",
                "/api/v1/ops/jobs/payment-reconciliation/executions/1")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/claims/1/payments").header("Idempotency-Key", "anon"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(policies, claims, reconciliation, recovery);
    }

    @Test void customerCannotReadOrExecuteAdministrativeReconciliation() throws Exception {
        mvc.perform(get("/api/v1/ops/jobs/payment-reconciliation/executions/1").with(user("42").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/ops/jobs/payment-reconciliation").with(user("42").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reconciliation, recovery);
        mvc.perform(get("/api/v1/ops/jobs/payment-reconciliation/executions/1").with(user("99").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(reconciliation).getExecution(1L);
    }

    @Test void controllersUseAuthenticatedIdentityNotClientSuppliedUserId() throws Exception {
        mvc.perform(get("/api/v1/policies/7?userId=1").with(user("42"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/claims/8?userId=1").with(user("42"))).andExpect(status().isOk());
        verify(policies).get(42L, 7L);
        verify(claims).get(42L, 8L);
    }
}
