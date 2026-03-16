package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAsyncProcessorTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private StripePaymentGateway stripePaymentGateway;

    @InjectMocks
    private PaymentAsyncProcessor paymentAsyncProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateCheckoutSessionAsynchronously() throws Exception {
        PaymentTransaction transaction = buildTransaction();
        transaction.setId(42L);

        when(paymentTransactionRepository.findById(42L)).thenReturn(Optional.of(transaction));
        when(stripePaymentGateway.createCheckoutSession(transaction, "https://success", "https://cancel"))
                .thenReturn(new StripePaymentGateway.CheckoutSessionData("cs_test_123", "https://stripe.test/checkout"));

        paymentAsyncProcessor.createCheckoutSessionAsync(42L, "https://success", "https://cancel").join();

        assertEquals(PaymentTransactionStatus.CHECKOUT_CREATED, transaction.getStatus());
        assertEquals("cs_test_123", transaction.getStripeCheckoutSessionId());
        assertEquals("https://stripe.test/checkout", transaction.getCheckoutUrl());
        verify(paymentTransactionRepository).save(transaction);
    }

    private PaymentTransaction buildTransaction() {
        User user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        DataProduct product = new DataProduct();
        product.setId(8L);
        product.setCode("FX-TICKS");
        product.setName("FX Tick Feed");
        product.setPrice(new BigDecimal("99.00"));
        product.setCurrency("usd");
        product.setAccessType(ProductAccessType.SUBSCRIPTION);
        product.setBillingInterval(BillingInterval.MONTHLY);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(user);
        transaction.setProduct(product);
        transaction.setAmount(product.getPrice());
        transaction.setCurrency(product.getCurrency());
        return transaction;
    }
}
