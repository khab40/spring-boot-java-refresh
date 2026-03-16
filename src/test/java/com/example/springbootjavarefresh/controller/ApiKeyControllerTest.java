package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.ApiKeyIssueResponse;
import com.example.springbootjavarefresh.dto.ApiKeyUsageSummaryResponse;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.ApiKeysService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiKeyController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiKeysService apiKeysService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldRegisterUserAndReturnApiKey() throws Exception {
        when(apiKeysService.registerAndIssueKey(any())).thenReturn(
                new ApiKeyIssueResponse(5L, "new@example.com", "mdr_secret", "mdr_secret", LocalDateTime.now(), null)
        );

        mockMvc.perform(post("/api/access/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "firstName": "New",
                                  "lastName": "User",
                                  "password": "super-secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5L))
                .andExpect(jsonPath("$.apiKey").value("mdr_secret"));
    }

    @Test
    void shouldRecordUsage() throws Exception {
        when(apiKeysService.recordUsage(any())).thenReturn(
                new ApiKeyUsageSummaryResponse(5L, 12L, new BigDecimal("10.00"), new BigDecimal("90.00"), 0, 5, 25L, 975L)
        );

        mockMvc.perform(post("/api/access/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "apiKey": "mdr_secret",
                                  "productId": 12,
                                  "usageType": "BATCH_DOWNLOAD",
                                  "megabytesUsed": 10.00,
                                  "requestCount": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchDownloadRemainingMb").value(90.00));
    }

    @Test
    void shouldGetUsageSummary() throws Exception {
        when(apiKeysService.getUsageSummary(eq("mdr_secret"), eq(12L))).thenReturn(
                new ApiKeyUsageSummaryResponse(5L, 12L, new BigDecimal("10.00"), new BigDecimal("90.00"), 0, 5, 25L, 975L)
        );

        mockMvc.perform(get("/api/access/usage/summary")
                        .param("apiKey", "mdr_secret")
                        .param("productId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(12L))
                .andExpect(jsonPath("$.payloadKilobytesRemaining").value(975L));
    }
}
