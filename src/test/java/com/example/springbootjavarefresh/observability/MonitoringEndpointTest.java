package com.example.springbootjavarefresh.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MonitoringEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalServiceProbe externalServiceProbe;

    @Test
    void shouldExposeReadinessHealthDetails() throws Exception {
        when(externalServiceProbe.probeFrontendUi())
                .thenReturn(new ServiceStatus("frontend-ui", true, "http://frontend:3000/api/health", 200, "reachable"));
        when(externalServiceProbe.probeAirflow())
                .thenReturn(new ServiceStatus("airflow", true, "http://airflow:8080/api/v2/monitor/health", 200, "reachable"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.catalog.status").value("UP"))
                .andExpect(jsonPath("$.components.payments.status").value("UP"));
    }

    @Test
    void shouldExposeMdlMonitoringSummary() throws Exception {
        when(externalServiceProbe.probeFrontendUi())
                .thenReturn(new ServiceStatus("frontend-ui", true, "http://frontend:3000/api/health", 200, "reachable"));
        when(externalServiceProbe.probeAirflow())
                .thenReturn(new ServiceStatus("airflow", false, "http://airflow:8080/api/v2/monitor/health", 503, "unexpected status"));

        mockMvc.perform(get("/actuator/mdl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrations.frontendUi.service").value("frontend-ui"))
                .andExpect(jsonPath("$.integrations.frontendUi.up").value(true))
                .andExpect(jsonPath("$.integrations.airflow.service").value("airflow"))
                .andExpect(jsonPath("$.integrations.airflow.up").value(false))
                .andExpect(jsonPath("$.catalog.catalogItems").isNumber())
                .andExpect(jsonPath("$.billing.transactionsByStatus").exists())
                .andExpect(jsonPath("$.system.users").isNumber())
                .andExpect(jsonPath("$.marketData.rows").isNumber());
    }
}
