package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("airflow")
public class AirflowHealthIndicator implements HealthIndicator {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public AirflowHealthIndicator(MdlMonitoringSummaryService monitoringSummaryService) {
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
