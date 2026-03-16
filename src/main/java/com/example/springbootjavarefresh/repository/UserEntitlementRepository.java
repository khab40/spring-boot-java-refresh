package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.UserEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, Long> {
    List<UserEntitlement> findByUserId(Long userId);
    Optional<UserEntitlement> findByUserIdAndProductId(Long userId, Long productId);
}
