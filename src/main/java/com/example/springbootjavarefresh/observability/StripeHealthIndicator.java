package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("stripe")
public class StripeHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public StripeHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        boolean configured = monitoringSummaryService.isStripeConfigured();
        Health.Builder builder = configured ? Health.up() : Health.outOfService();
        return builder
                .withDetail("configured", configured)
                .withDetail("mode", monitoringSummaryService.getStripeMode())
                .withDetail("pendingTransactions", monitoringSummaryService.getPaymentCount(com.example.springbootjavarefresh.entity.PaymentTransactionStatus.PENDING))
                .withDetail("checkoutCreatedTransactions", monitoringSummaryService.getPaymentCount(com.example.springbootjavarefresh.entity.PaymentTransactionStatus.CHECKOUT_CREATED))
                .withDetail("failedTransactions", monitoringSummaryService.getPaymentCount(com.example.springbootjavarefresh.entity.PaymentTransactionStatus.FAILED))
                .build();
    }
}
