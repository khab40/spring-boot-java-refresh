package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.Subscription;
import com.example.springbootjavarefresh.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllSubscriptions() {
        Subscription sub1 = new Subscription("user1", "AAPL");
        Subscription sub2 = new Subscription("user2", "GOOGL");
        when(subscriptionRepository.findAll()).thenReturn(Arrays.asList(sub1, sub2));

        List<Subscription> result = subscriptionService.getAllSubscriptions();

        assertEquals(2, result.size());
        verify(subscriptionRepository, times(1)).findAll();
    }

    @Test
    void testSubscribe() {
        when(subscriptionRepository.findByUserIdAndSymbol("user1", "AAPL")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(new Subscription("user1", "AAPL"));

        boolean result = subscriptionService.subscribe("user1", "AAPL");

        assertTrue(result);
        verify(subscriptionRepository, times(1)).findByUserIdAndSymbol("user1", "AAPL");
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void testSubscribeAlreadySubscribed() {
        Subscription existing = new Subscription("user1", "AAPL");
        existing.setActive(true);
        when(subscriptionRepository.findByUserIdAndSymbol("user1", "AAPL")).thenReturn(Optional.of(existing));

        boolean result = subscriptionService.subscribe("user1", "AAPL");

        assertFalse(result);
        verify(subscriptionRepository, times(1)).findByUserIdAndSymbol("user1", "AAPL");
        verify(subscriptionRepository, times(0)).save(any(Subscription.class));
    }

    @Test
    void testUnsubscribe() {
        Subscription existing = new Subscription("user1", "AAPL");
        existing.setActive(true);
        when(subscriptionRepository.findByUserIdAndSymbol("user1", "AAPL")).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(existing);

        boolean result = subscriptionService.unsubscribe("user1", "AAPL");

        assertTrue(result);
        verify(subscriptionRepository, times(1)).findByUserIdAndSymbol("user1", "AAPL");
        verify(subscriptionRepository, times(1)).save(existing);
        assertFalse(existing.getActive());
    }

    @Test
    void testUnsubscribeNotSubscribed() {
        when(subscriptionRepository.findByUserIdAndSymbol("user1", "AAPL")).thenReturn(Optional.empty());

        boolean result = subscriptionService.unsubscribe("user1", "AAPL");

        assertFalse(result);
        verify(subscriptionRepository, times(1)).findByUserIdAndSymbol("user1", "AAPL");
        verify(subscriptionRepository, times(0)).save(any(Subscription.class));
    }
}