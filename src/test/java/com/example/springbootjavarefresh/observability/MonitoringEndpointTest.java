package com.example.springbootjavarefresh.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringEndpointTest {

    @Mock
    private MdlMonitoringSummaryService monitoringSummaryService;

    @InjectMocks
    private MdlMonitoringEndpoint monitoringEndpoint;

    @Test
    void shouldExposeMonitoringSummary() {
        Map<String, Object> expectedSummary = Map.of(
                "integrations", Map.of(
                        "frontendUi", Map.of("service", "frontend-ui", "up", true),
                        "airflow", Map.of("service", "airflow", "up", false)
                )
        );

        when(monitoringSummaryService.buildSummary()).thenReturn(expectedSummary);

        assertEquals(expectedSummary, monitoringEndpoint.summary());
    }
}
