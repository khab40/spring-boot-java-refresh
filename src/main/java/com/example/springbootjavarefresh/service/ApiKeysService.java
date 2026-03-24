package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.ApiKeyIssueResponse;
import com.example.springbootjavarefresh.dto.ApiKeyLoginRequest;
import com.example.springbootjavarefresh.dto.ApiKeyUsageRequest;
import com.example.springbootjavarefresh.dto.ApiKeyUsageSummaryResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.entity.ApiKey;
import com.example.springbootjavarefresh.entity.ApiKeyStatus;
import com.example.springbootjavarefresh.entity.ApiKeyUsageRecord;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.UsageType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.ApiKeyRepository;
import com.example.springbootjavarefresh.repository.ApiKeyUsageRecordRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
public class ApiKeysService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserService userService;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageRecordRepository apiKeyUsageRecordRepository;
    private final DataProductRepository dataProductRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeysService(
            UserService userService,
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyUsageRecordRepository apiKeyUsageRecordRepository,
            DataProductRepository dataProductRepository,
            UserEntitlementRepository userEntitlementRepository,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyUsageRecordRepository = apiKeyUsageRecordRepository;
        this.dataProductRepository = dataProductRepository;
        this.userEntitlementRepository = userEntitlementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ApiKeyIssueResponse registerAndIssueKey(CreateUserRequest request) {
        User user = userService.createUser(request);
        return issueKeyForUser(user);
    }

    public ApiKeyIssueResponse loginAndIssueKey(ApiKeyLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + request.getEmail()));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return issueKeyForUser(user);
    }

    public ApiKeyUsageSummaryResponse recordUsage(ApiKeyUsageRequest request) {
        ApiKey apiKey = resolveActiveApiKey(request.getApiKey());
        DataProduct product = dataProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Data product not found: " + request.getProductId()));
        UserEntitlement entitlement = resolveActiveEntitlement(apiKey.getUser().getId(), product.getId());

        BigDecimal megabytesUsed = safeBigDecimal(request.getMegabytesUsed());
        int realtimeSubscriptionsUsed = safeInteger(request.getRealtimeSubscriptionsUsed());
        long payloadKilobytesUsed = safeLong(request.getPayloadKilobytesUsed());

        validateUsageRequest(request.getUsageType(), megabytesUsed, realtimeSubscriptionsUsed);
        enforceLimits(entitlement, product, megabytesUsed, realtimeSubscriptionsUsed, payloadKilobytesUsed);

        entitlement.setBatchDownloadUsedMb(entitlement.getBatchDownloadUsedMb().add(megabytesUsed));
        entitlement.setRealtimeSubscriptionsUsed(entitlement.getRealtimeSubscriptionsUsed() + realtimeSubscriptionsUsed);
        entitlement.setPayloadKilobytesUsed(entitlement.getPayloadKilobytesUsed() + payloadKilobytesUsed);
        userEntitlementRepository.save(entitlement);

        ApiKeyUsageRecord usageRecord = new ApiKeyUsageRecord();
        usageRecord.setApiKey(apiKey);
        usageRecord.setUser(apiKey.getUser());
        usageRecord.setProduct(product);
        usageRecord.setUsageType(request.getUsageType());
        usageRecord.setMegabytesUsed(megabytesUsed);
        usageRecord.setPayloadKilobytesUsed(payloadKilobytesUsed);
        usageRecord.setRealtimeSubscriptionsUsed(realtimeSubscriptionsUsed);
        usageRecord.setRequestCount(request.getRequestCount());
        usageRecord.setNotes(request.getNotes());
        apiKeyUsageRecordRepository.save(usageRecord);

        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        return toUsageSummary(entitlement, product);
    }

    public ApiKeyUsageSummaryResponse getUsageSummary(String apiKeyValue, Long productId) {
        ApiKey apiKey = resolveActiveApiKey(apiKeyValue);
        DataProduct product = dataProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Data product not found: " + productId));
        UserEntitlement entitlement = resolveActiveEntitlement(apiKey.getUser().getId(), productId);
        return toUsageSummary(entitlement, product);
    }

    public ApiKeyIssueResponse issueKeyForUser(User user) {
        String rawToken = generateRawToken();
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setKeyPrefix(rawToken.substring(0, 12));
        apiKey.setKeyHash(hash(rawToken));
        apiKey.setExpiresAt(resolveKeyExpiry(user.getId()));
        ApiKey saved = apiKeyRepository.save(apiKey);
        return new ApiKeyIssueResponse(
                user.getId(),
                user.getEmail(),
                rawToken,
                saved.getKeyPrefix(),
                saved.getIssuedAt(),
                saved.getExpiresAt()
        );
    }

    private ApiKey resolveActiveApiKey(String apiKeyValue) {
        ApiKey apiKey = apiKeyRepository.findByKeyHash(hash(apiKeyValue))
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new IllegalArgumentException("API key is not active");
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("API key has expired");
        }
        return apiKey;
    }

    private UserEntitlement resolveActiveEntitlement(Long userId, Long productId) {
        UserEntitlement entitlement = userEntitlementRepository
                .findFirstByUserIdAndProductIdAndStatusOrderByGrantedAtDescIdDesc(userId, productId, EntitlementStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No entitlement found for user " + userId + " and product " + productId));
        if (entitlement.getExpiresAt() != null && entitlement.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Entitlement has expired");
        }
        return entitlement;
    }

    private void enforceLimits(
            UserEntitlement entitlement,
            DataProduct product,
            BigDecimal megabytesUsed,
            int realtimeSubscriptionsUsed,
            long payloadKilobytesUsed) {
        BigDecimal nextBatchUsage = entitlement.getBatchDownloadUsedMb().add(megabytesUsed);
        BigDecimal allocatedBatchLimit = allocatedBatchLimit(product, entitlement);
        if (allocatedBatchLimit != null && nextBatchUsage.compareTo(allocatedBatchLimit) > 0) {
            throw new IllegalArgumentException("Batch download limit exceeded for product " + product.getCode());
        }

        int nextRealtimeUsage = entitlement.getRealtimeSubscriptionsUsed() + realtimeSubscriptionsUsed;
        Integer allocatedRealtimeLimit = allocatedRealtimeLimit(product, entitlement);
        if (allocatedRealtimeLimit != null && nextRealtimeUsage > allocatedRealtimeLimit) {
            throw new IllegalArgumentException("Realtime subscription limit exceeded for product " + product.getCode());
        }

        long nextPayloadUsage = entitlement.getPayloadKilobytesUsed() + payloadKilobytesUsed;
        Long allocatedPayloadLimit = allocatedPayloadLimit(product, entitlement);
        if (allocatedPayloadLimit != null && nextPayloadUsage > allocatedPayloadLimit) {
            throw new IllegalArgumentException("Realtime payload limit exceeded for product " + product.getCode());
        }
    }

    private void validateUsageRequest(UsageType usageType, BigDecimal megabytesUsed, int realtimeSubscriptionsUsed) {
        if (usageType == UsageType.BATCH_DOWNLOAD && megabytesUsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Batch download usage must include a positive megabyte value");
        }
        if (usageType == UsageType.REALTIME_SUBSCRIPTION && realtimeSubscriptionsUsed <= 0) {
            throw new IllegalArgumentException("Realtime subscription usage must include a positive subscription count");
        }
    }

    private ApiKeyUsageSummaryResponse toUsageSummary(UserEntitlement entitlement, DataProduct product) {
        return new ApiKeyUsageSummaryResponse(
                entitlement.getUser().getId(),
                product.getId(),
                entitlement.getBatchDownloadUsedMb(),
                remainingBatchMb(entitlement, product),
                entitlement.getRealtimeSubscriptionsUsed(),
                remainingRealtimeSubscriptions(entitlement, product),
                entitlement.getPayloadKilobytesUsed(),
                remainingPayloadKb(entitlement, product)
        );
    }

    private BigDecimal remainingBatchMb(UserEntitlement entitlement, DataProduct product) {
        BigDecimal allocatedLimit = allocatedBatchLimit(product, entitlement);
        if (allocatedLimit == null) {
            return null;
        }
        return allocatedLimit.subtract(entitlement.getBatchDownloadUsedMb());
    }

    private Integer remainingRealtimeSubscriptions(UserEntitlement entitlement, DataProduct product) {
        Integer allocatedLimit = allocatedRealtimeLimit(product, entitlement);
        if (allocatedLimit == null) {
            return null;
        }
        return allocatedLimit - entitlement.getRealtimeSubscriptionsUsed();
    }

    private Long remainingPayloadKb(UserEntitlement entitlement, DataProduct product) {
        Long allocatedLimit = allocatedPayloadLimit(product, entitlement);
        if (allocatedLimit == null) {
            return null;
        }
        return allocatedLimit - entitlement.getPayloadKilobytesUsed();
    }

    private BigDecimal allocatedBatchLimit(DataProduct product, UserEntitlement entitlement) {
        if (product.getBatchDownloadLimitMb() == null) {
            return null;
        }
        return product.getBatchDownloadLimitMb().multiply(BigDecimal.valueOf(resolvePurchasedUnits(entitlement)));
    }

    private Integer allocatedRealtimeLimit(DataProduct product, UserEntitlement entitlement) {
        if (product.getRealtimeSubscriptionLimit() == null) {
            return null;
        }
        return product.getRealtimeSubscriptionLimit() * resolvePurchasedUnits(entitlement);
    }

    private Long allocatedPayloadLimit(DataProduct product, UserEntitlement entitlement) {
        if (product.getMaxRealtimePayloadKb() == null) {
            return null;
        }
        return (long) product.getMaxRealtimePayloadKb() * resolvePurchasedUnits(entitlement);
    }

    private int resolvePurchasedUnits(UserEntitlement entitlement) {
        Integer purchasedUnits = entitlement.getPurchasedUnits();
        return purchasedUnits == null || purchasedUnits < 1 ? 1 : purchasedUnits;
    }

    private LocalDateTime resolveKeyExpiry(Long userId) {
        List<UserEntitlement> activeEntitlements = userEntitlementRepository.findByUserId(userId).stream()
                .filter(entitlement -> entitlement.getStatus() == EntitlementStatus.ACTIVE)
                .toList();
        boolean hasNonExpiringEntitlement = activeEntitlements.stream().anyMatch(entitlement -> entitlement.getExpiresAt() == null);
        if (hasNonExpiringEntitlement) {
            return null;
        }
        return activeEntitlements.stream()
                .map(UserEntitlement::getExpiresAt)
                .filter(expiry -> expiry != null)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now().plusDays(30));
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "mdr_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
