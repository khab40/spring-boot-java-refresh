package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.Subscription;
import com.example.springbootjavarefresh.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

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