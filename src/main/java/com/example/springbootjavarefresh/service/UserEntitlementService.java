package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionItem;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserEntitlementService {

    private final UserEntitlementRepository userEntitlementRepository;

    public UserEntitlementService(UserEntitlementRepository userEntitlementRepository) {
        this.userEntitlementRepository = userEntitlementRepository;
    }

    public List<UserEntitlement> getEntitlementsByUserId(Long userId) {
        return userEntitlementRepository.findByUserId(userId);
    }

    public UserEntitlement grantEntitlement(PaymentTransaction transaction) {
        if (transaction.getItems() != null && !transaction.getItems().isEmpty()) {
            UserEntitlement latestEntitlement = null;
            for (PaymentTransactionItem item : transaction.getItems()) {
                latestEntitlement = grantEntitlement(transaction, item.getProduct(), item.getQuantity());
            }
            return latestEntitlement;
        }

        return grantEntitlement(transaction, transaction.getProduct(), 1);
    }

    private UserEntitlement grantEntitlement(PaymentTransaction transaction, DataProduct product, int purchasedUnits) {
        UserEntitlement entitlement = userEntitlementRepository
                .findFirstByUserIdAndProductIdOrderByGrantedAtDescIdDesc(transaction.getUser().getId(), product.getId())
                .orElseGet(UserEntitlement::new);

        entitlement.setUser(transaction.getUser());
        entitlement.setProduct(product);
        entitlement.setAccessType(product.getAccessType());
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setGrantedAt(LocalDateTime.now());
        entitlement.setSourceTransactionId(transaction.getId());
        entitlement.setPurchasedUnits((entitlement.getPurchasedUnits() == null ? 0 : entitlement.getPurchasedUnits()) + purchasedUnits);
        if (entitlement.getBatchDownloadUsedMb() == null) {
            entitlement.setBatchDownloadUsedMb(BigDecimal.ZERO);
        }
        if (entitlement.getRealtimeSubscriptionsUsed() == null) {
            entitlement.setRealtimeSubscriptionsUsed(0);
        }
        if (entitlement.getPayloadKilobytesUsed() == null) {
            entitlement.setPayloadKilobytesUsed(0L);
        }

        if (product.getBillingInterval() == BillingInterval.MONTHLY) {
            entitlement.setExpiresAt(LocalDateTime.now().plusMonths(1));
        } else if (product.getBillingInterval() == BillingInterval.YEARLY) {
            entitlement.setExpiresAt(LocalDateTime.now().plusYears(1));
        } else {
            entitlement.setExpiresAt(null);
        }

        return userEntitlementRepository.save(entitlement);
    }
}
