package com.example.springbootjavarefresh.observability;

import com.example.springbootjavarefresh.entity.ApiKeyStatus;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.Subscription;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.ApiKeyRepository;
import com.example.springbootjavarefresh.repository.ApiKeyUsageRecordRepository;
import com.example.springbootjavarefresh.repository.DataCatalogItemRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.example.springbootjavarefresh.repository.SubscriptionRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import com.example.springbootjavarefresh.service.MarketDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MdlMonitoringSummaryService {

    private final DataCatalogItemRepository dataCatalogItemRepository;
    private final DataProductRepository dataProductRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApiKeyUsageRecordRepository apiKeyUsageRecordRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final ExternalServiceProbe externalServiceProbe;
    private final String stripeApiKey;

    public MdlMonitoringSummaryService(
            DataCatalogItemRepository dataCatalogItemRepository,
            DataProductRepository dataProductRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            ApiKeyUsageRecordRepository apiKeyUsageRecordRepository,
            UserEntitlementRepository userEntitlementRepository,
            SubscriptionRepository subscriptionRepository,
            ApiKeyRepository apiKeyRepository,
            UserRepository userRepository,
            MarketDataService marketDataService,
            ExternalServiceProbe externalServiceProbe,
            @Value("${stripe.api-key:}") String stripeApiKey) {
        this.dataCatalogItemRepository = dataCatalogItemRepository;
        this.dataProductRepository = dataProductRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.apiKeyUsageRecordRepository = apiKeyUsageRecordRepository;
        this.userEntitlementRepository = userEntitlementRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.marketDataService = marketDataService;
        this.externalServiceProbe = externalServiceProbe;
        this.stripeApiKey = stripeApiKey;
    }

    public Map<String, Object> buildSummary() {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "integrations", buildIntegrations(),
                "catalog", buildCatalog(),
                "billing", buildBilling(),
                "usage", buildUsage(),
                "subscriptions", buildSubscriptions(),
                "entitlements", buildEntitlements(),
                "system", buildSystem(),
                "marketData", buildMarketData()
        );
    }

    public long getCatalogItemCount() {
        return dataCatalogItemRepository.count();
    }

    public long getActiveCatalogItemCount() {
        return dataCatalogItemRepository.findByActiveTrueOrderByNameAsc().size();
    }

    public long getOfferCount() {
        return dataProductRepository.count();
    }

    public long getActiveOfferCount() {
        return dataProductRepository.findByActiveTrue().size();
    }

    public long getPaymentCount(PaymentTransactionStatus status) {
        return paymentTransactionRepository.findAll().stream()
                .filter(transaction -> transaction.getStatus() == status)
                .count();
    }

    public double getSuccessfulPaymentAmount() {
        return paymentTransactionRepository.findAll().stream()
                .filter(transaction -> transaction.getStatus() == PaymentTransactionStatus.SUCCEEDED)
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    public long getUsageRecordCount() {
        return apiKeyUsageRecordRepository.count();
    }

    public double getMegabytesUsedTotal() {
        return apiKeyUsageRecordRepository.findAll().stream()
                .map(record -> record.getMegabytesUsed() == null ? BigDecimal.ZERO : record.getMegabytesUsed())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    public long getRealtimeSubscriptionsUsedTotal() {
        return apiKeyUsageRecordRepository.findAll().stream()
                .mapToLong(record -> record.getRealtimeSubscriptionsUsed() == null ? 0 : record.getRealtimeSubscriptionsUsed())
                .sum();
    }

    public long getPayloadKilobytesUsedTotal() {
        return apiKeyUsageRecordRepository.findAll().stream()
                .mapToLong(record -> record.getPayloadKilobytesUsed() == null ? 0 : record.getPayloadKilobytesUsed())
                .sum();
    }

    public long getRequestCountTotal() {
        return apiKeyUsageRecordRepository.findAll().stream()
                .mapToLong(record -> record.getRequestCount() == null ? 0 : record.getRequestCount())
                .sum();
    }

    public long getActiveEntitlementCount() {
        return userEntitlementRepository.findAll().stream()
                .filter(entitlement -> entitlement.getStatus() == EntitlementStatus.ACTIVE)
                .count();
    }

    public long getActiveSubscriptionCount() {
        return subscriptionRepository.findAll().stream()
                .filter(subscription -> Boolean.TRUE.equals(subscription.getActive()))
                .count();
    }

    public long getSubscriptionUserCount() {
        return subscriptionRepository.findAll().stream()
                .filter(subscription -> Boolean.TRUE.equals(subscription.getActive()))
                .map(Subscription::getUserId)
                .distinct()
                .count();
    }

    public long getActiveApiKeyCount() {
        return apiKeyRepository.findAll().stream()
                .filter(apiKey -> apiKey.getStatus() == ApiKeyStatus.ACTIVE)
                .count();
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getMarketDataRowCount() {
        return marketDataService.getAllMarketData().size();
    }

    public boolean isStripeConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank();
    }

    public String getStripeMode() {
        if (!isStripeConfigured()) {
            return "unconfigured";
        }
        return stripeApiKey.startsWith("sk_test_") ? "sandbox" : "live";
    }

    public ServiceStatus frontendStatus() {
        return externalServiceProbe.probeFrontendUi();
    }

    public ServiceStatus airflowStatus() {
        return externalServiceProbe.probeAirflow();
    }

    public long getServiceHealthStatus(String serviceName) {
        return switch (serviceName) {
            case "frontend-ui" -> frontendStatus().up() ? 1 : 0;
            case "airflow" -> airflowStatus().up() ? 1 : 0;
            case "stripe" -> isStripeConfigured() ? 1 : 0;
            default -> 0;
        };
    }

    public long getDiskFreeBytes() {
        try {
            FileStore fileStore = Files.getFileStore(Path.of("."));
            return fileStore.getUsableSpace();
        } catch (Exception exception) {
            return -1L;
        }
    }

    public long getDiskTotalBytes() {
        try {
            FileStore fileStore = Files.getFileStore(Path.of("."));
            return fileStore.getTotalSpace();
        } catch (Exception exception) {
            return -1L;
        }
    }

    private Map<String, Object> buildIntegrations() {
        return Map.of(
                "frontendUi", frontendStatus(),
                "airflow", airflowStatus(),
                "stripe", Map.of(
                        "configured", isStripeConfigured(),
                        "mode", getStripeMode()
                )
        );
    }

    private Map<String, Object> buildCatalog() {
        return Map.of(
                "catalogItems", getCatalogItemCount(),
                "activeCatalogItems", getActiveCatalogItemCount(),
                "offers", getOfferCount(),
                "activeOffers", getActiveOfferCount()
        );
    }

    private Map<String, Object> buildBilling() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (PaymentTransactionStatus status : PaymentTransactionStatus.values()) {
            byStatus.put(status.name(), getPaymentCount(status));
        }

        return Map.of(
                "transactionsByStatus", byStatus,
                "successfulAmount", getSuccessfulPaymentAmount()
        );
    }

    private Map<String, Object> buildUsage() {
        return Map.of(
                "records", getUsageRecordCount(),
                "requestCount", getRequestCountTotal(),
                "megabytesUsed", getMegabytesUsedTotal(),
                "payloadKilobytesUsed", getPayloadKilobytesUsedTotal(),
                "realtimeSubscriptionsUsed", getRealtimeSubscriptionsUsedTotal(),
                "topUsers", topUsageUsers(5)
        );
    }

    private Map<String, Object> buildSubscriptions() {
        return Map.of(
                "totalSubscriptions", subscriptionRepository.count(),
                "activeSubscriptions", getActiveSubscriptionCount(),
                "usersWithActiveSubscriptions", getSubscriptionUserCount(),
                "topUsers", topSubscriptionUsers(5)
        );
    }

    private Map<String, Object> buildEntitlements() {
        return Map.of(
                "activeEntitlements", getActiveEntitlementCount(),
                "totalEntitlements", userEntitlementRepository.count()
        );
    }

    private Map<String, Object> buildSystem() {
        return Map.of(
                "users", getTotalUserCount(),
                "activeApiKeys", getActiveApiKeyCount(),
                "diskFreeBytes", getDiskFreeBytes(),
                "diskTotalBytes", getDiskTotalBytes()
        );
    }

    private Map<String, Object> buildMarketData() {
        return Map.of(
                "runtime", marketDataService.getRuntimeStatus(),
                "rows", getMarketDataRowCount()
        );
    }

    private List<Map<String, Object>> topUsageUsers(int limit) {
        Map<Long, User> usersById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return apiKeyUsageRecordRepository.findAll().stream()
                .collect(Collectors.groupingBy(record -> record.getUser().getId()))
                .entrySet().stream()
                .map(entry -> {
                    long requestCount = entry.getValue().stream()
                            .mapToLong(record -> record.getRequestCount() == null ? 0 : record.getRequestCount())
                            .sum();
                    double megabytesUsed = entry.getValue().stream()
                            .map(record -> record.getMegabytesUsed() == null ? BigDecimal.ZERO : record.getMegabytesUsed())
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .doubleValue();
                    User user = usersById.get(entry.getKey());
                    return Map.<String, Object>of(
                            "userId", entry.getKey(),
                            "email", user == null ? "unknown" : user.getEmail(),
                            "requestCount", requestCount,
                            "megabytesUsed", megabytesUsed
                    );
                })
                .sorted(Comparator.comparingLong(item -> -((Number) item.get("requestCount")).longValue()))
                .limit(limit)
                .toList();
    }

    private List<Map<String, Object>> topSubscriptionUsers(int limit) {
        return subscriptionRepository.findAll().stream()
                .filter(subscription -> Boolean.TRUE.equals(subscription.getActive()))
                .collect(Collectors.groupingBy(Subscription::getUserId))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "userId", entry.getKey(),
                        "activeSubscriptions", entry.getValue().size(),
                        "symbols", entry.getValue().stream().map(Subscription::getSymbol).sorted().toList()
                ))
                .sorted(Comparator.comparingInt(item -> -((Number) item.get("activeSubscriptions")).intValue()))
                .limit(limit)
                .toList();
    }
}
