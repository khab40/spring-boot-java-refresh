package com.example.springbootjavarefresh.observability;

import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.service.MarketDataService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("frontendUi")
class FrontendUiHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    FrontendUiHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        ServiceStatus status = monitoringSummaryService.frontendStatus();
        Health.Builder builder = status.up() ? Health.up() : Health.down();
        return builder
                .withDetail("target", status.target())
                .withDetail("httpStatus", status.httpStatus())
                .withDetail("detail", status.detail())
                .build();
    }
}

@Component("airflow")
class AirflowHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    AirflowHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        ServiceStatus status = monitoringSummaryService.airflowStatus();
        Health.Builder builder = status.up() ? Health.up() : Health.down();
        return builder
                .withDetail("target", status.target())
                .withDetail("httpStatus", status.httpStatus())
                .withDetail("detail", status.detail())
                .build();
    }
}

@Component("stripe")
class StripeHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    StripeHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        boolean configured = monitoringSummaryService.isStripeConfigured();
        Health.Builder builder = configured ? Health.up() : Health.outOfService();
        return builder
                .withDetail("configured", configured)
                .withDetail("mode", monitoringSummaryService.getStripeMode())
                .withDetail("pendingTransactions", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.PENDING))
                .withDetail("checkoutCreatedTransactions", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.CHECKOUT_CREATED))
                .withDetail("failedTransactions", monitoringSummaryService.getPaymentCount(PaymentTransactionStatus.FAILED))
                .build();
    }
}

@Component("catalog")
class CatalogHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    CatalogHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("catalogItems", monitoringSummaryService.getCatalogItemCount())
                .withDetail("activeCatalogItems", monitoringSummaryService.getActiveCatalogItemCount())
                .withDetail("offers", monitoringSummaryService.getOfferCount())
                .withDetail("activeOffers", monitoringSummaryService.getActiveOfferCount())
                .build();
    }
}

@Component("payments")
class PaymentsHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    PaymentsHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
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

@Component("usage")
class UsageHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    UsageHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("records", monitoringSummaryService.getUsageRecordCount())
                .withDetail("requestCount", monitoringSummaryService.getRequestCountTotal())
                .withDetail("megabytesUsed", monitoringSummaryService.getMegabytesUsedTotal())
                .withDetail("payloadKilobytesUsed", monitoringSummaryService.getPayloadKilobytesUsedTotal())
                .withDetail("realtimeSubscriptionsUsed", monitoringSummaryService.getRealtimeSubscriptionsUsedTotal())
                .build();
    }
}

@Component("subscriptions")
class SubscriptionsHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    SubscriptionsHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("activeSubscriptions", monitoringSummaryService.getActiveSubscriptionCount())
                .withDetail("usersWithActiveSubscriptions", monitoringSummaryService.getSubscriptionUserCount())
                .build();
    }
}

@Component("marketData")
class MarketDataHealthIndicator implements HealthIndicator {

    private final MarketDataService marketDataService;
    private final MdlMonitoringSummaryService monitoringSummaryService;

    MarketDataHealthIndicator(MarketDataService marketDataService, MdlMonitoringSummaryService monitoringSummaryService) {
        this.marketDataService = marketDataService;
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("runtime", marketDataService.getRuntimeStatus())
                .withDetail("rows", monitoringSummaryService.getMarketDataRowCount())
                .build();
    }
}
