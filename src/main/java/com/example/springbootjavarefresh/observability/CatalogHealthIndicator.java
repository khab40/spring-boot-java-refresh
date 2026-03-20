package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("catalog")
public class CatalogHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public CatalogHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
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
