package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.ApiKeyLoginRequest;
import com.example.springbootjavarefresh.dto.ApiKeyUsageRequest;
import com.example.springbootjavarefresh.dto.ApiKeyUsageSummaryResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.entity.ApiKey;
import com.example.springbootjavarefresh.entity.ApiKeyStatus;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.UsageType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.ApiKeyRepository;
import com.example.springbootjavarefresh.repository.ApiKeyUsageRecordRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeysServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyUsageRecordRepository apiKeyUsageRecordRepository;

    @Mock
    private DataProductRepository dataProductRepository;

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApiKeysService apiKeysService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldRegisterAndIssueApiKey() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("keys@example.com");
        request.setFirstName("Key");
        request.setLastName("Holder");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(5L);
        user.setEmail("keys@example.com");
        when(userService.createUser(request)).thenReturn(user);
        when(userEntitlementRepository.findByUserId(5L)).thenReturn(java.util.List.of());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey apiKey = invocation.getArgument(0);
            apiKey.setId(9L);
            apiKey.setIssuedAt(LocalDateTime.now());
            return apiKey;
        });

        var response = apiKeysService.registerAndIssueKey(request);

        assertEquals(5L, response.userId());
        assertEquals("keys@example.com", response.email());
        assertNotNull(response.apiKey());
        assertNotNull(response.keyPrefix());
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    void shouldIssueApiKeyForLoginOfExistingUser() {
        ApiKeyLoginRequest request = new ApiKeyLoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(3L);
        user.setEmail("login@example.com");
        user.setPasswordHash("hashed-secret");
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("super-secret", "hashed-secret")).thenReturn(true);
        when(userEntitlementRepository.findByUserId(3L)).thenReturn(java.util.List.of());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = apiKeysService.loginAndIssueKey(request);

        assertEquals(3L, response.userId());
        assertEquals("login@example.com", response.email());
    }

    @Test
    void shouldRecordUsageAndUpdateEntitlementCounters() {
        User user = new User();
        user.setId(2L);
        user.setEmail("usage@example.com");

        DataProduct product = new DataProduct();
        product.setId(12L);
        product.setCode("FX-PRO");
        product.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        product.setBatchDownloadLimitMb(new BigDecimal("500.00"));
        product.setRealtimeSubscriptionLimit(5);
        product.setMaxRealtimePayloadKb(2000);

        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUser(user);
        entitlement.setProduct(product);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setPurchasedUnits(2);
        entitlement.setBatchDownloadUsedMb(new BigDecimal("100.00"));
        entitlement.setRealtimeSubscriptionsUsed(1);
        entitlement.setPayloadKilobytesUsed(300L);

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setKeyHash(hashForTest("raw-key"));

        ApiKeyUsageRequest request = new ApiKeyUsageRequest();
        request.setApiKey("raw-key");
        request.setProductId(12L);
        request.setUsageType(UsageType.BATCH_DOWNLOAD);
        request.setMegabytesUsed(new BigDecimal("50.00"));
        request.setPayloadKilobytesUsed(20L);
        request.setRequestCount(1);

        when(apiKeyRepository.findByKeyHash(hashForTest("raw-key"))).thenReturn(Optional.of(apiKey));
        when(dataProductRepository.findById(12L)).thenReturn(Optional.of(product));
        when(userEntitlementRepository.findFirstByUserIdAndProductIdAndStatusOrderByGrantedAtDescIdDesc(2L, 12L, EntitlementStatus.ACTIVE))
                .thenReturn(Optional.of(entitlement));

        ApiKeyUsageSummaryResponse summary = apiKeysService.recordUsage(request);

        assertEquals(new BigDecimal("150.00"), entitlement.getBatchDownloadUsedMb());
        assertEquals(320L, entitlement.getPayloadKilobytesUsed());
        assertEquals(new BigDecimal("850.00"), summary.batchDownloadRemainingMb());
        verify(apiKeyUsageRecordRepository).save(any());
        verify(userEntitlementRepository).save(entitlement);
    }

    @Test
    void shouldRejectUsageThatExceedsProductLimit() {
        User user = new User();
        user.setId(2L);

        DataProduct product = new DataProduct();
        product.setId(12L);
        product.setCode("REALTIME");
        product.setRealtimeSubscriptionLimit(1);
        product.setMaxRealtimePayloadKb(1024);

        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUser(user);
        entitlement.setProduct(product);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setBatchDownloadUsedMb(BigDecimal.ZERO);
        entitlement.setRealtimeSubscriptionsUsed(1);
        entitlement.setPayloadKilobytesUsed(0L);

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setKeyHash(hashForTest("realtime-key"));

        ApiKeyUsageRequest request = new ApiKeyUsageRequest();
        request.setApiKey("realtime-key");
        request.setProductId(12L);
        request.setUsageType(UsageType.REALTIME_SUBSCRIPTION);
        request.setRealtimeSubscriptionsUsed(1);

        when(apiKeyRepository.findByKeyHash(hashForTest("realtime-key"))).thenReturn(Optional.of(apiKey));
        when(dataProductRepository.findById(12L)).thenReturn(Optional.of(product));
        when(userEntitlementRepository.findFirstByUserIdAndProductIdAndStatusOrderByGrantedAtDescIdDesc(2L, 12L, EntitlementStatus.ACTIVE))
                .thenReturn(Optional.of(entitlement));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> apiKeysService.recordUsage(request));
        assertEquals("Realtime subscription limit exceeded for product REALTIME", exception.getMessage());
    }

    private String hashForTest(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
