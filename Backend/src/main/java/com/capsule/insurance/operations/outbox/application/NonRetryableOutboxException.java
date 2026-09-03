package com.capsule.insurance.operations.outbox.application;

public class NonRetryableOutboxException extends RuntimeException {

    public NonRetryableOutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
