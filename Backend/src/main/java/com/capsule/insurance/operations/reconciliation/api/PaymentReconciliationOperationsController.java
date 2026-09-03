package com.capsule.insurance.operations.reconciliation.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.operations.reconciliation.application.PaymentReconciliationBatchService;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.operations.reconciliation.dto.StartPaymentReconciliationRequest;
import com.capsule.insurance.operations.recovery.application.OperationsRecoveryService;
import com.capsule.insurance.operations.recovery.dto.PaymentReconciliationRecoveryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/jobs/payment-reconciliation")
public class PaymentReconciliationOperationsController {

    private final PaymentReconciliationBatchService service;
    private final OperationsRecoveryService recoveryService;

    public PaymentReconciliationOperationsController(
            PaymentReconciliationBatchService service,
            OperationsRecoveryService recoveryService
    ) {
        this.service = service;
        this.recoveryService = recoveryService;
    }

    @PostMapping
    public ApiResponse<PaymentReconciliationRecoveryResponse> run(
            @Valid @RequestBody StartPaymentReconciliationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "결제 미확정 건 대사가 완료되었습니다.",
                recoveryService.runPaymentReconciliation(
                        AuthenticatedUser.id(authentication),
                        request.reason(),
                        request.chunkSize(),
                        request.staleAfterSeconds()
                )
        );
    }

    @GetMapping("/executions/{jobExecutionId}")
    public ApiResponse<PaymentReconciliationExecutionResponse> getExecution(
            @PathVariable Long jobExecutionId
    ) {
        return ApiResponse.success(service.getExecution(jobExecutionId));
    }
}
