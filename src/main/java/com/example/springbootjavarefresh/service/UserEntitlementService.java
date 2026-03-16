package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import org.springframework.stereotype.Service;

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
        UserEntitlement entitlement = userEntitlementRepository
                .findByUserIdAndProductId(transaction.getUser().getId(), transaction.getProduct().getId())
                .orElseGet(UserEntitlement::new);

        entitlement.setUser(transaction.getUser());
        entitlement.setProduct(transaction.getProduct());
        entitlement.setAccessType(transaction.getProduct().getAccessType());
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setGrantedAt(LocalDateTime.now());
        entitlement.setSourceTransactionId(transaction.getId());

        if (transaction.getProduct().getBillingInterval() == BillingInterval.MONTHLY) {
            entitlement.setExpiresAt(LocalDateTime.now().plusMonths(1));
        } else if (transaction.getProduct().getBillingInterval() == BillingInterval.YEARLY) {
            entitlement.setExpiresAt(LocalDateTime.now().plusYears(1));
        } else {
            entitlement.setExpiresAt(null);
        }

        return userEntitlementRepository.save(entitlement);
    }
}
