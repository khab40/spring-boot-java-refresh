package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.PaymentCheckoutRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionItem;
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

    @Mock
    private StripePaymentGateway stripePaymentGateway;

    @Mock
    private UserEntitlementService userEntitlementService;

    @Mock
    private CatalogPricingService catalogPricingService;

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
        PaymentCheckoutRequest.CheckoutLineItem item = new PaymentCheckoutRequest.CheckoutLineItem();
        item.setProductId(9L);
        item.setQuantity(2);
        request.setItems(java.util.List.of(item));
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
        DataCatalogItem catalogItem = new DataCatalogItem();
        catalogItem.setId(12L);
        product.setCatalogItem(catalogItem);

        PaymentTransaction saved = new PaymentTransaction();
        saved.setId(77L);
        saved.setUser(user);
        saved.setProduct(product);
        saved.setAmount(new BigDecimal("118.00"));
        saved.setCurrency(product.getCurrency());
        saved.setStatus(PaymentTransactionStatus.PENDING);

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(dataProductRepository.findById(9L)).thenReturn(Optional.of(product));
        when(catalogPricingService.quote(org.mockito.ArgumentMatchers.eq(catalogItem), org.mockito.ArgumentMatchers.eq(product), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new CatalogPricingService.CatalogPriceQuote(
                        new BigDecimal("71.00"),
                        1,
                        1,
                        java.time.LocalDate.of(2026, 3, 1),
                        java.time.LocalDate.of(2026, 3, 1),
                        "DEFAULT",
                        "base 71.00"
                ));
        when(paymentTransactionRepository.save(org.mockito.ArgumentMatchers.any(PaymentTransaction.class))).thenReturn(saved);

        PaymentTransaction result = paymentService.initiateCheckout(request);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        PaymentTransaction persisted = captor.getValue();
        assertEquals(user, persisted.getUser());
        assertEquals(product, persisted.getProduct());
        assertEquals(new BigDecimal("142.00"), persisted.getAmount());
        assertEquals("usd", persisted.getCurrency());
        assertEquals(PaymentTransactionStatus.PENDING, persisted.getStatus());
        assertEquals(1, persisted.getItems().size());
        PaymentTransactionItem persistedItem = persisted.getItems().get(0);
        assertEquals(2, persistedItem.getQuantity());
        assertEquals(new BigDecimal("71.00"), persistedItem.getUnitPrice());
        assertEquals(new BigDecimal("142.00"), persistedItem.getLineAmount());
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

    @Test
    void shouldRejectMixedAccessTypesInSingleCart() {
        PaymentCheckoutRequest request = new PaymentCheckoutRequest();
        request.setUserId(4L);
        request.setSuccessUrl("https://example.com/success");
        request.setCancelUrl("https://example.com/cancel");

        PaymentCheckoutRequest.CheckoutLineItem subscriptionItem = new PaymentCheckoutRequest.CheckoutLineItem();
        subscriptionItem.setProductId(9L);
        subscriptionItem.setQuantity(1);
        PaymentCheckoutRequest.CheckoutLineItem oneTimeItem = new PaymentCheckoutRequest.CheckoutLineItem();
        oneTimeItem.setProductId(10L);
        oneTimeItem.setQuantity(1);
        request.setItems(java.util.List.of(subscriptionItem, oneTimeItem));

        User user = new User();
        user.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        DataProduct subscription = new DataProduct();
        subscription.setId(9L);
        subscription.setCode("STREAM");
        subscription.setPrice(new BigDecimal("59.00"));
        subscription.setCurrency("usd");
        subscription.setAccessType(ProductAccessType.SUBSCRIPTION);
        subscription.setBillingInterval(BillingInterval.MONTHLY);
        subscription.setCatalogItem(new DataCatalogItem());

        DataProduct oneTime = new DataProduct();
        oneTime.setId(10L);
        oneTime.setCode("SNAPSHOT");
        oneTime.setPrice(new BigDecimal("99.00"));
        oneTime.setCurrency("usd");
        oneTime.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        oneTime.setBillingInterval(BillingInterval.ONE_TIME);
        oneTime.setCatalogItem(new DataCatalogItem());

        when(dataProductRepository.findById(9L)).thenReturn(Optional.of(subscription));
        when(dataProductRepository.findById(10L)).thenReturn(Optional.of(oneTime));
        when(catalogPricingService.quote(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(subscription), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new CatalogPricingService.CatalogPriceQuote(
                        new BigDecimal("59.00"),
                        1,
                        1,
                        java.time.LocalDate.of(2026, 3, 1),
                        java.time.LocalDate.of(2026, 3, 1),
                        "DEFAULT",
                        "base 59.00"
                ));
        when(catalogPricingService.quote(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(oneTime), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new CatalogPricingService.CatalogPriceQuote(
                        new BigDecimal("99.00"),
                        1,
                        1,
                        java.time.LocalDate.of(2026, 3, 1),
                        java.time.LocalDate.of(2026, 3, 1),
                        "DEFAULT",
                        "base 99.00"
                ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.initiateCheckout(request));
        assertEquals("Stripe checkout does not support mixing subscriptions and one-time purchases", ex.getMessage());
    }

    @Test
    void shouldMarkCheckoutCreatedTransactionAsSucceededWhenStripeReportsPaid() throws Exception {
        User user = new User();
        user.setId(4L);

        DataProduct product = new DataProduct();
        product.setId(9L);
        product.setCode("DEPTH");
        product.setName("Depth Feed");
        product.setPrice(new BigDecimal("59.00"));
        product.setCurrency("usd");
        product.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        product.setBillingInterval(BillingInterval.ONE_TIME);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(77L);
        transaction.setUser(user);
        transaction.setProduct(product);
        transaction.setAmount(new BigDecimal("59.00"));
        transaction.setCurrency("usd");
        transaction.setStatus(PaymentTransactionStatus.CHECKOUT_CREATED);
        transaction.setStripeCheckoutSessionId("cs_test_123");

        when(stripePaymentGateway.getCheckoutSessionStatus("cs_test_123"))
                .thenReturn(new StripePaymentGateway.CheckoutSessionStatus(
                        "cs_test_123",
                        "complete",
                        "paid",
                        "https://checkout.stripe.test/session"));
        when(paymentTransactionRepository.save(org.mockito.ArgumentMatchers.any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction refreshed = paymentService.refreshTransactionStatusFromStripe(transaction);

        assertEquals(PaymentTransactionStatus.SUCCEEDED, refreshed.getStatus());
        assertEquals("https://checkout.stripe.test/session", refreshed.getCheckoutUrl());
        verify(userEntitlementService).grantEntitlement(transaction);
        verify(paymentTransactionRepository).save(transaction);
    }
}
