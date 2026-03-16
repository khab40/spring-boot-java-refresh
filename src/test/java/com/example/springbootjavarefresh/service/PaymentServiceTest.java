package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.PaymentCheckoutRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DataProductRepository dataProductRepository;

    @Mock
    private PaymentAsyncProcessor paymentAsyncProcessor;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldInitiateCheckoutAndTriggerAsyncProcessor() {
        PaymentCheckoutRequest request = new PaymentCheckoutRequest();
        request.setUserId(4L);
        request.setProductId(9L);
        request.setSuccessUrl("https://example.com/success");
        request.setCancelUrl("https://example.com/cancel");

        User user = new User();
        user.setId(4L);
        user.setEmail("paying@example.com");

        DataProduct product = new DataProduct();
        product.setId(9L);
        product.setName("Depth Feed");
        product.setPrice(new BigDecimal("59.00"));
        product.setCurrency("usd");
        product.setAccessType(ProductAccessType.SUBSCRIPTION);
        product.setBillingInterval(BillingInterval.MONTHLY);

        PaymentTransaction saved = new PaymentTransaction();
        saved.setId(77L);
        saved.setUser(user);
        saved.setProduct(product);
        saved.setAmount(product.getPrice());
        saved.setCurrency(product.getCurrency());
        saved.setStatus(PaymentTransactionStatus.PENDING);

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(dataProductRepository.findById(9L)).thenReturn(Optional.of(product));
        when(paymentTransactionRepository.save(org.mockito.ArgumentMatchers.any(PaymentTransaction.class))).thenReturn(saved);

        PaymentTransaction result = paymentService.initiateCheckout(request);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        PaymentTransaction persisted = captor.getValue();
        assertEquals(user, persisted.getUser());
        assertEquals(product, persisted.getProduct());
        assertEquals(new BigDecimal("59.00"), persisted.getAmount());
        assertEquals("usd", persisted.getCurrency());
        assertEquals(PaymentTransactionStatus.PENDING, persisted.getStatus());
        verify(paymentAsyncProcessor).createCheckoutSessionAsync(77L, "https://example.com/success", "https://example.com/cancel");
        assertEquals(77L, result.getId());
    }

    @Test
    void shouldRejectUnknownUser() {
        PaymentCheckoutRequest request = new PaymentCheckoutRequest();
        request.setUserId(999L);
        request.setProductId(9L);
        request.setSuccessUrl("https://example.com/success");
        request.setCancelUrl("https://example.com/cancel");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.initiateCheckout(request));
        assertEquals("User not found: 999", ex.getMessage());
    }
}
