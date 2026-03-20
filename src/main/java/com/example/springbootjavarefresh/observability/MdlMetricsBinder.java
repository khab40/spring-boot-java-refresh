package com.example.springbootjavarefresh.observability;

import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class MdlMetricsBinder implements MeterBinder {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public MdlMetricsBinder(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("mdl_catalog_items_total", monitoringSummaryService, MdlMonitoringSummaryService::getCatalogItemCount).register(registry);
        Gauge.builder("mdl_catalog_active_items_total", monitoringSummaryService, MdlMonitoringSummaryService::getActiveCatalogItemCount).register(registry);
        Gauge.builder("mdl_catalog_offers_total", monitoringSummaryService, MdlMonitoringSummaryService::getOfferCount).register(registry);
        Gauge.builder("mdl_catalog_active_offers_total", monitoringSummaryService, MdlMonitoringSummaryService::getActiveOfferCount).register(registry);
        Gauge.builder("mdl_usage_records_total", monitoringSummaryService, MdlMonitoringSummaryService::getUsageRecordCount).register(registry);
        Gauge.builder("mdl_usage_requests_total", monitoringSummaryService, MdlMonitoringSummaryService::getRequestCountTotal).register(registry);
        Gauge.builder("mdl_usage_megabytes_total", monitoringSummaryService, MdlMonitoringSummaryService::getMegabytesUsedTotal).register(registry);
        Gauge.builder("mdl_usage_payload_kilobytes_total", monitoringSummaryService, MdlMonitoringSummaryService::getPayloadKilobytesUsedTotal).register(registry);
        Gauge.builder("mdl_usage_realtime_subscriptions_total", monitoringSummaryService, MdlMonitoringSummaryService::getRealtimeSubscriptionsUsedTotal).register(registry);
        Gauge.builder("mdl_active_entitlements_total", monitoringSummaryService, MdlMonitoringSummaryService::getActiveEntitlementCount).register(registry);
        Gauge.builder("mdl_active_subscriptions_total", monitoringSummaryService, MdlMonitoringSummaryService::getActiveSubscriptionCount).register(registry);
        Gauge.builder("mdl_active_subscription_users_total", monitoringSummaryService, MdlMonitoringSummaryService::getSubscriptionUserCount).register(registry);
        Gauge.builder("mdl_active_api_keys_total", monitoringSummaryService, MdlMonitoringSummaryService::getActiveApiKeyCount).register(registry);
        Gauge.builder("mdl_users_total", monitoringSummaryService, MdlMonitoringSummaryService::getTotalUserCount).register(registry);
        Gauge.builder("mdl_market_data_rows_total", monitoringSummaryService, MdlMonitoringSummaryService::getMarketDataRowCount).register(registry);
        Gauge.builder("mdl_billing_successful_amount_total", monitoringSummaryService, MdlMonitoringSummaryService::getSuccessfulPaymentAmount).register(registry);
        Gauge.builder("mdl_system_disk_free_bytes", monitoringSummaryService, MdlMonitoringSummaryService::getDiskFreeBytes).register(registry);
        Gauge.builder("mdl_system_disk_total_bytes", monitoringSummaryService, MdlMonitoringSummaryService::getDiskTotalBytes).register(registry);
        Gauge.builder("mdl_service_health_status", monitoringSummaryService, service -> service.getServiceHealthStatus("frontend-ui"))
                .tag("service", "frontend-ui")
                .register(registry);
        Gauge.builder("mdl_service_health_status", monitoringSummaryService, service -> service.getServiceHealthStatus("airflow"))
                .tag("service", "airflow")
                .register(registry);
        Gauge.builder("mdl_service_health_status", monitoringSummaryService, service -> service.getServiceHealthStatus("stripe"))
                .tag("service", "stripe")
                .register(registry);

        for (PaymentTransactionStatus status : PaymentTransactionStatus.values()) {
            Gauge.builder("mdl_payment_transactions_total", monitoringSummaryService, service -> service.getPaymentCount(status))
                    .tag("status", status.name())
                    .register(registry);
        }
    }
}
