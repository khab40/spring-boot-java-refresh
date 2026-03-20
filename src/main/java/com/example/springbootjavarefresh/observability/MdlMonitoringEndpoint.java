package com.example.springbootjavarefresh.observability;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "mdl")
public class MdlMonitoringEndpoint {

    private final MdlMonitoringSummaryService monitoringSummaryService;

    public MdlMonitoringEndpoint(MdlMonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @ReadOperation
    public Map<String, Object> summary() {
        return monitoringSummaryService.buildSummary();
    }
}
