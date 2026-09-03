package com.capsule.insurance.payment.application.port;

import com.capsule.insurance.payment.domain.FinancialInterfaceMessage;

public interface FinancialInterfaceJournalRepository {

    void append(FinancialInterfaceMessage message);
}
