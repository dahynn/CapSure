package com.capsule.insurance.operations.outbox.application.port;

import com.capsule.insurance.operations.outbox.domain.OutboxEvent;

public interface OutboxEventHandler {

    void handle(OutboxEvent event);
}
