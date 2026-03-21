package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.OtdDeliveryFileResponse;
import com.example.springbootjavarefresh.dto.OtdDeliveryResponse;
import com.example.springbootjavarefresh.entity.DataDeliveryStatus;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.OtdDeliveryService;
import com.example.springbootjavarefresh.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OtdDeliveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class OtdDeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OtdDeliveryService otdDeliveryService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldCreateDeliveryForAuthenticatedUser() throws Exception {
        User user = userEntity();
        OtdDeliveryResponse response = response();

        when(userService.getUserByEmail("delivery@example.com")).thenReturn(Optional.of(user));
        when(otdDeliveryService.createDelivery(eq(7L), any())).thenReturn(response);

        OtdDeliveryController controller = new OtdDeliveryController(otdDeliveryService, userService);
        OtdDeliveryResponse body = controller.createDelivery(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()),
                new com.example.springbootjavarefresh.dto.OtdDeliveryRequest(11L, "SELECT * FROM market_data WHERE symbol = 'AAPL'")
        ).getBody();

        assertEquals(91L, body.deliveryId());
        assertEquals("AAPL-OTD", body.productCode());
        assertEquals("aapl.parquet", body.files().getFirst().fileName());
        verify(otdDeliveryService).createDelivery(eq(7L), any());
    }

    @Test
    void shouldRejectInvalidCreateDeliveryRequest() throws Exception {
        User user = userEntity();
        when(userService.getUserByEmail("delivery@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/market-data/otd-deliveries")
                        .with(user("delivery@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": null,
                                  "sql": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListCurrentUsersDeliveries() throws Exception {
        User user = userEntity();
        when(userService.getUserByEmail("delivery@example.com")).thenReturn(Optional.of(user));
        when(otdDeliveryService.getDeliveriesForUser(7L)).thenReturn(List.of(response()));

        OtdDeliveryController controller = new OtdDeliveryController(otdDeliveryService, userService);
        List<OtdDeliveryResponse> deliveries = controller.myDeliveries(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        ).getBody();

        assertEquals(1, deliveries.size());
        assertEquals(91L, deliveries.getFirst().deliveryId());
        assertEquals(DataDeliveryStatus.READY, deliveries.getFirst().status());
        assertEquals(128L, deliveries.getFirst().files().getFirst().sizeBytes());
    }

    private User userEntity() {
        User user = new User();
        user.setId(7L);
        user.setEmail("delivery@example.com");
        user.setFirstName("Delivery");
        user.setLastName("User");
        user.setPasswordHash("ignored");
        return user;
    }

    private OtdDeliveryResponse response() {
        return new OtdDeliveryResponse(
                91L,
                11L,
                "AAPL-OTD",
                "AAPL On-Time Delivery",
                DataDeliveryStatus.READY,
                "SELECT * FROM market_data WHERE symbol = 'AAPL'",
                1,
                1,
                128L,
                new BigDecimal("0.01"),
                new BigDecimal("49.99"),
                LocalDateTime.parse("2026-03-20T12:00:00"),
                List.of(new OtdDeliveryFileResponse(
                        "aapl.parquet",
                        "otd/7/2026/03/20/120000/aapl.parquet",
                        "http://localhost:9000/signed/aapl.parquet",
                        128L,
                        LocalDateTime.parse("2026-03-21T12:00:00")
                ))
        );
    }
}
