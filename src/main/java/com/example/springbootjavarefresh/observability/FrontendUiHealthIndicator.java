package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("frontendUi")
public class FrontendUiHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public FrontendUiHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
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
