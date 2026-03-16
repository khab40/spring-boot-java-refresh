package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentWebhookServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserEntitlementService userEntitlementService;

    private PaymentWebhookService paymentWebhookService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentWebhookService = new PaymentWebhookService(
                paymentTransactionRepository,
                userEntitlementService,
                new ObjectMapper(),
                ""
        );
    }

    @Test
    void shouldMarkTransactionSucceededAndGrantEntitlement() throws Exception {
        PaymentTransaction transaction = buildTransaction();
        when(paymentTransactionRepository.findById(42L)).thenReturn(Optional.of(transaction));

        paymentWebhookService.handleWebhook(successPayload(), null);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals(PaymentTransactionStatus.SUCCEEDED, captor.getValue().getStatus());
        verify(userEntitlementService).grantEntitlement(transaction);
    }

    @Test
    void shouldMarkTransactionFailedFromAsyncFailureEvent() throws Exception {
        PaymentTransaction transaction = buildTransaction();
        transaction.setStripeCheckoutSessionId("cs_test_failed");
        when(paymentTransactionRepository.findByStripeCheckoutSessionId("cs_test_failed")).thenReturn(Optional.of(transaction));

        paymentWebhookService.handleWebhook(failurePayload(), null);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals(PaymentTransactionStatus.FAILED, captor.getValue().getStatus());
        assertEquals("Stripe checkout did not complete successfully", captor.getValue().getErrorMessage());
        verify(userEntitlementService, never()).grantEntitlement(transaction);
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
        transaction.setId(42L);
        transaction.setUser(user);
        transaction.setProduct(product);
        transaction.setAmount(product.getPrice());
        transaction.setCurrency(product.getCurrency());
        transaction.setStatus(PaymentTransactionStatus.CHECKOUT_CREATED);
        return transaction;
    }

    private String successPayload() {
        return """
                {
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_success",
                      "clientReferenceId": "42"
                    }
                  }
                }
                """;
    }

    private String failurePayload() {
        return """
                {
                  "type": "checkout.session.async_payment_failed",
                  "data": {
                    "object": {
                      "id": "cs_test_failed"
                    }
                  }
                }
                """;
    }
}
