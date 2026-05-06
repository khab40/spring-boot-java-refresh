package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.Subscription;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {
private MockMvc mockMvc;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private UserDetailsService userDetailsService;
    @InjectMocks
    private SubscriptionController subscriptionController;



    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController).build();
    }

@Test
    void testGetAllSubscriptions() throws Exception {
        Subscription sub = new Subscription("user1", "AAPL");
        when(subscriptionService.getAllSubscriptions()).thenReturn(Arrays.asList(sub));

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user1"));

        verify(subscriptionService, times(1)).getAllSubscriptions();
    }

    @Test
    void testSubscribe() throws Exception {
        when(subscriptionService.subscribe("user1", "AAPL")).thenReturn(true);

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .param("userId", "user1")
                        .param("symbol", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscribed successfully"));

        verify(subscriptionService, times(1)).subscribe("user1", "AAPL");
    }

    @Test
    void testSubscribeAlreadySubscribed() throws Exception {
        when(subscriptionService.subscribe("user1", "AAPL")).thenReturn(false);

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .param("userId", "user1")
                        .param("symbol", "AAPL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Already subscribed"));

        verify(subscriptionService, times(1)).subscribe("user1", "AAPL");
    }

    @Test
    void testUnsubscribe() throws Exception {
        when(subscriptionService.unsubscribe("user1", "AAPL")).thenReturn(true);

        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .param("userId", "user1")
                        .param("symbol", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("Unsubscribed successfully"));

        verify(subscriptionService, times(1)).unsubscribe("user1", "AAPL");
    }
}