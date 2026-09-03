package com.capsule.insurance.operations.reconciliation.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.operations.reconciliation.application.PaymentReconciliationBatchService;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.operations.reconciliation.dto.StartPaymentReconciliationRequest;
import jakarta.validation.Valid;
import java.time.Duration;
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

    public PaymentReconciliationOperationsController(PaymentReconciliationBatchService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PaymentReconciliationExecutionResponse> run(
            @Valid @RequestBody StartPaymentReconciliationRequest request
    ) {
        return ApiResponse.success(
                "결제 미확정 건 대사가 완료되었습니다.",
                service.run(
                        request.instanceKey(),
                        PaymentReconciliationRunOptions.production(
                                request.chunkSize(),
                                Duration.ofSeconds(request.staleAfterSeconds())
                        )
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
