package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.ApiKeyUsageRecord;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.ApiKeyRepository;
import com.example.springbootjavarefresh.repository.ApiKeyUsageRecordRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdminAuditServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DataProductRepository dataProductRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyUsageRecordRepository apiKeyUsageRecordRepository;

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @InjectMocks
    private AdminAuditService adminAuditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldBuildDashboardWithRecentActivity() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setFirstName("Admin");
        user.setLastName("User");

        DataProduct product = new DataProduct();
        product.setId(2L);
        product.setCode("FX-STREAM");
        product.setName("FX Stream");

        PaymentTransaction payment = new PaymentTransaction();
        payment.setId(3L);
        payment.setUser(user);
        payment.setProduct(product);

        ApiKeyUsageRecord usageRecord = new ApiKeyUsageRecord();
        usageRecord.setId(4L);
        usageRecord.setUser(user);
        usageRecord.setProduct(product);

        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(5L);
        entitlement.setUser(user);
        entitlement.setProduct(product);
        entitlement.setStatus(EntitlementStatus.ACTIVE);

        when(userRepository.count()).thenReturn(1L);
        when(dataProductRepository.count()).thenReturn(2L);
        when(paymentTransactionRepository.count()).thenReturn(3L);
        when(apiKeyRepository.count()).thenReturn(4L);
        when(apiKeyUsageRecordRepository.count()).thenReturn(5L);
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(user));
        when(paymentTransactionRepository.findAll(any(Sort.class))).thenReturn(List.of(payment));
        when(apiKeyUsageRecordRepository.findAll(any(Sort.class))).thenReturn(List.of(usageRecord));
        when(userEntitlementRepository.findAll()).thenReturn(List.of(entitlement));
        when(userEntitlementRepository.findAll(any(Sort.class))).thenReturn(List.of(entitlement));

        var dashboard = adminAuditService.getDashboard();

        assertEquals(1L, dashboard.totalUsers());
        assertEquals(2L, dashboard.totalProducts());
        assertEquals(1, dashboard.recentUsers().size());
        assertEquals(1, dashboard.recentPayments().size());
        assertEquals(1, dashboard.recentUsage().size());
        assertEquals(1L, dashboard.activeEntitlements());
    }
}
