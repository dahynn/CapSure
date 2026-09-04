package com.capsule.insurance.premiumcollection.infra;

import com.capsule.insurance.premiumcollection.application.PremiumNoticeGateway;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPremiumNoticeGateway implements PremiumNoticeGateway {
    @Override
    public boolean deliverSimulation(long receivableId) {
        return true;
    }
}
