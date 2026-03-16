package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.AdminDashboardResponse;
import com.example.springbootjavarefresh.dto.AdminEntitlementSummaryResponse;
import com.example.springbootjavarefresh.dto.AdminPaymentSummaryResponse;
import com.example.springbootjavarefresh.dto.AdminUsageSummaryResponse;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.repository.ApiKeyRepository;
import com.example.springbootjavarefresh.repository.ApiKeyUsageRecordRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuditService {

    private static final int DEFAULT_LIMIT = 10;

    private final UserRepository userRepository;
    private final DataProductRepository dataProductRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageRecordRepository apiKeyUsageRecordRepository;
    private final UserEntitlementRepository userEntitlementRepository;

    public AdminAuditService(
            UserRepository userRepository,
            DataProductRepository dataProductRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyUsageRecordRepository apiKeyUsageRecordRepository,
            UserEntitlementRepository userEntitlementRepository) {
        this.userRepository = userRepository;
        this.dataProductRepository = dataProductRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyUsageRecordRepository = apiKeyUsageRecordRepository;
        this.userEntitlementRepository = userEntitlementRepository;
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                userRepository.count(),
                dataProductRepository.count(),
                paymentTransactionRepository.count(),
                apiKeyRepository.count(),
                apiKeyUsageRecordRepository.count(),
                userEntitlementRepository.findAll().stream()
                        .filter(entitlement -> entitlement.getStatus() == EntitlementStatus.ACTIVE)
                        .count(),
                userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                        .limit(DEFAULT_LIMIT)
                        .map(UserProfileResponse::fromUser)
                        .toList(),
                paymentTransactionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                        .limit(DEFAULT_LIMIT)
                        .map(AdminPaymentSummaryResponse::fromTransaction)
                        .toList(),
                apiKeyUsageRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt")).stream()
                        .limit(DEFAULT_LIMIT)
                        .map(AdminUsageSummaryResponse::fromUsage)
                        .toList(),
                userEntitlementRepository.findAll(Sort.by(Sort.Direction.DESC, "grantedAt")).stream()
                        .limit(DEFAULT_LIMIT)
                        .map(AdminEntitlementSummaryResponse::fromEntitlement)
                        .toList()
        );
    }

    public List<AdminPaymentSummaryResponse> getRecentPayments() {
        return paymentTransactionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .limit(25)
                .map(AdminPaymentSummaryResponse::fromTransaction)
                .toList();
    }

    public List<AdminUsageSummaryResponse> getRecentUsage() {
        return apiKeyUsageRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt")).stream()
                .limit(50)
                .map(AdminUsageSummaryResponse::fromUsage)
                .toList();
    }
}
