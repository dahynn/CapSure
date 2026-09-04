package com.capsule.insurance.premiumcollection.application;

/** Local simulation only. A real delivery adapter requires a separate outbox workflow. */
public interface PremiumNoticeGateway {
    boolean deliverSimulation(long receivableId);
}
