package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionItem;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserEntitlementServiceTest {

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @InjectMocks
    private UserEntitlementService userEntitlementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldGrantMonthlyEntitlement() {
        PaymentTransaction transaction = buildTransaction(BillingInterval.MONTHLY);
        when(userEntitlementRepository.findFirstByUserIdAndProductIdOrderByGrantedAtDescIdDesc(5L, 8L)).thenReturn(Optional.empty());
        when(userEntitlementRepository.save(org.mockito.ArgumentMatchers.any(UserEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeGrant = LocalDateTime.now();
        UserEntitlement result = userEntitlementService.grantEntitlement(transaction);
        LocalDateTime afterGrant = LocalDateTime.now();

        verify(userEntitlementRepository).save(result);
        assertEquals(EntitlementStatus.ACTIVE, result.getStatus());
        assertEquals(ProductAccessType.SUBSCRIPTION, result.getAccessType());
        assertEquals(42L, result.getSourceTransactionId());
        assertEquals(2, result.getPurchasedUnits());
        assertNotNull(result.getGrantedAt());
        assertNotNull(result.getExpiresAt());
        assertTrue(!result.getGrantedAt().isBefore(beforeGrant) && !result.getGrantedAt().isAfter(afterGrant));
        Duration validity = Duration.between(result.getGrantedAt(), result.getExpiresAt());
        assertTrue(validity.toDays() >= 28 && validity.toDays() <= 31);
    }

    @Test
    void shouldReuseExistingEntitlementForOneTimePurchase() {
        PaymentTransaction transaction = buildTransaction(BillingInterval.ONE_TIME);
        transaction.getProduct().setAccessType(ProductAccessType.ONE_TIME_PURCHASE);

        UserEntitlement existing = new UserEntitlement();
        existing.setId(91L);
        when(userEntitlementRepository.findFirstByUserIdAndProductIdOrderByGrantedAtDescIdDesc(5L, 8L)).thenReturn(Optional.of(existing));
        when(userEntitlementRepository.save(org.mockito.ArgumentMatchers.any(UserEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserEntitlement result = userEntitlementService.grantEntitlement(transaction);

        assertEquals(91L, result.getId());
        assertEquals(ProductAccessType.ONE_TIME_PURCHASE, result.getAccessType());
        assertEquals(EntitlementStatus.ACTIVE, result.getStatus());
        assertEquals(3, result.getPurchasedUnits());
        assertEquals(null, result.getExpiresAt());
    }

    private PaymentTransaction buildTransaction(BillingInterval billingInterval) {
        User user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");

        DataProduct product = new DataProduct();
        product.setId(8L);
        product.setCode("FX-TICKS");
        product.setName("FX Tick Feed");
        product.setPrice(new BigDecimal("99.00"));
        product.setCurrency("usd");
        product.setAccessType(ProductAccessType.SUBSCRIPTION);
        product.setBillingInterval(billingInterval);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(42L);
        transaction.setUser(user);
        transaction.setProduct(product);
        transaction.setAmount(product.getPrice());
        transaction.setCurrency(product.getCurrency());
        PaymentTransactionItem item = new PaymentTransactionItem();
        item.setTransaction(transaction);
        item.setProduct(product);
        item.setQuantity(billingInterval == BillingInterval.ONE_TIME ? 3 : 2);
        item.setUnitPrice(product.getPrice());
        item.setLineAmount(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        item.setCurrency(product.getCurrency());
        transaction.setItems(java.util.List.of(item));
        return transaction;
    }
}
