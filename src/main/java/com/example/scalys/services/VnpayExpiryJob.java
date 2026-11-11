package com.example.scalys.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VnpayExpiryJob {
    private final VnpayPaymentService payments;
    private final OrderFollowupService followups;

    public VnpayExpiryJob(VnpayPaymentService payments, OrderFollowupService followups) {
        this.payments = payments;
        this.followups = followups;
    }

    @Scheduled(fixedDelay = 60_000)
    public void releaseAbandonedOrders() {
        for (Long id : payments.expiredPendingOrderIds()) payments.expirePending(id);
        for (Long id : payments.expiredDepositOrderIds()) payments.expireDeposit(id);
        for (Long id : payments.expiredBalanceIds()) payments.expireBalance(id);
        for (Long id : followups.expiredCodIds()) followups.expireCod(id);
    }
}
