package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AdminDashboardResponse;
import com.example.springbootjavarefresh.dto.AdminEntitlementSummaryResponse;
import com.example.springbootjavarefresh.dto.AdminPaymentSummaryResponse;
import com.example.springbootjavarefresh.dto.AdminUsageSummaryResponse;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.AuthProvider;
import com.example.springbootjavarefresh.entity.UserRole;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.AdminAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
private MockMvc mockMvc;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private UserDetailsService userDetailsService;
    @InjectMocks
    private AdminController adminController;



    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

@Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldReturnAdminDashboard() throws Exception {
        AdminDashboardResponse response = new AdminDashboardResponse(
                3,
                4,
                5,
                6,
                7,
                2,
                List.of(new UserProfileResponse(1L, "admin@example.com", "Admin", "User", null, null, null, UserRole.ADMIN, AuthProvider.LOCAL, true, null, null)),
                List.of(new AdminPaymentSummaryResponse(10L, 1L, "trader@example.com", 20L, "FX-STREAM", new BigDecimal("99.00"), "usd", null, null, null, LocalDateTime.now(), LocalDateTime.now())),
                List.of(new AdminUsageSummaryResponse(11L, 1L, "trader@example.com", 20L, "FX-STREAM", null, new BigDecimal("25.00"), 0L, 0, 1, null, LocalDateTime.now())),
                List.of(new AdminEntitlementSummaryResponse(12L, 1L, "trader@example.com", 20L, "FX-STREAM", null, null, LocalDateTime.now(), null, new BigDecimal("0.00"), 0, 0L))
        );

        when(adminAuditService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.recentUsers[0].email").value("admin@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldReturnRecentUsage() throws Exception {
        when(adminAuditService.getRecentUsage()).thenReturn(List.of(
                new AdminUsageSummaryResponse(11L, 1L, "trader@example.com", 20L, "FX-STREAM", null, new BigDecimal("25.00"), 0L, 0, 1, null, LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/admin/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("trader@example.com"));
    }
}