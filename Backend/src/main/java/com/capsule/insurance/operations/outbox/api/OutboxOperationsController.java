package com.capsule.insurance.operations.outbox.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.operations.outbox.application.OutboxRelayService;
import com.capsule.insurance.operations.outbox.dto.OutboxRelayRunResponse;
import com.capsule.insurance.operations.outbox.dto.OutboxSummaryResponse;
import com.capsule.insurance.operations.outbox.dto.ReplayOutboxRequest;
import com.capsule.insurance.operations.recovery.application.OperationsRecoveryService;
import com.capsule.insurance.operations.recovery.dto.DlqRecoveryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/outbox")
public class OutboxOperationsController {

    private final OutboxRelayService relayService;
    private final OperationsRecoveryService recoveryService;

    public OutboxOperationsController(
            OutboxRelayService relayService,
            OperationsRecoveryService recoveryService
    ) {
        this.relayService = relayService;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/relay")
    public ApiResponse<OutboxRelayRunResponse> relay(
            @RequestParam(defaultValue = "100") int batchSize
    ) {
        return ApiResponse.success(relayService.relay(batchSize));
    }

    @PostMapping("/{eventId}/replay")
    public ApiResponse<DlqRecoveryResponse> replay(
            @PathVariable String eventId,
            @Valid @RequestBody ReplayOutboxRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(recoveryService.replayDlq(
                eventId,
                AuthenticatedUser.id(authentication),
                request.reason()
        ));
    }

    @GetMapping("/summary")
    public ApiResponse<OutboxSummaryResponse> summary() {
        return ApiResponse.success(relayService.summary());
    }
}
