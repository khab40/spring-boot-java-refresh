package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("subscriptions")
public class SubscriptionsHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public SubscriptionsHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
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
