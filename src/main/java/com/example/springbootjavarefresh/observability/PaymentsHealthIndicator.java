package com.example.springbootjavarefresh.observability;

import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("payments")
public class PaymentsHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public PaymentsHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("pending", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.PENDING))
                .withDetail("checkoutCreated", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.CHECKOUT_CREATED))
                .withDetail("succeeded", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.SUCCEEDED))
                .withDetail("failed", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.FAILED))
                .withDetail("successfulAmount", monitoringSummaryService.getSuccessfulPaymentAmount())
                .build();
    }
}
