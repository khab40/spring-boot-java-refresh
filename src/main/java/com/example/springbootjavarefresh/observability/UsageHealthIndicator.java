package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("usage")
public class UsageHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public UsageHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
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
