
package com.skating.platform.payment.scheduler;

import com.skating.platform.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment.reconciliation",
        name = "enabled",
        havingValue = "true"
)
public class PaymentReconciliationScheduler {

    private final PaymentService paymentService;

    @Scheduled(
            fixedDelayString = "${payment.reconciliation.fixed-delay:30000}",
            initialDelayString = "${payment.reconciliation.initial-delay:10000}"
    )
    public void reconcilePendingPayments() {

        log.info("Starting payment reconciliation");

        try {
            paymentService.reconcilePendingPayments();
        } catch (RuntimeException exception) {
            log.error("Payment reconciliation failed", exception);
        }
    }
}
