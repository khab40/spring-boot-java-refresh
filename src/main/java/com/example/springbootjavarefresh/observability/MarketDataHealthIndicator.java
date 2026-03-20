package com.example.springbootjavarefresh.observability;

import com.example.springbootjavarefresh.service.MarketDataService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("marketData")
public class MarketDataHealthIndicator implements HealthIndicator {

    private final MarketDataService marketDataService;
    private final MdlMonitoringSummaryService monitoringSummaryService;

    public MarketDataHealthIndicator(MarketDataService marketDataService, MdlMonitoringSummaryService monitoringSummaryService) {
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
