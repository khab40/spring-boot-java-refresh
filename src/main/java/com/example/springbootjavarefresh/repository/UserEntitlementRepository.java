package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, Long> {
    List<UserEntitlement> findByUserId(Long userId);
    List<UserEntitlement> findByUserIdAndProductIdOrderByGrantedAtDescIdDesc(Long userId, Long productId);
    Optional<UserEntitlement> findFirstByUserIdAndProductIdOrderByGrantedAtDescIdDesc(Long userId, Long productId);
    Optional<UserEntitlement> findFirstByUserIdAndProductIdAndStatusOrderByGrantedAtDescIdDesc(
            Long userId,
            Long productId,
            EntitlementStatus status
    );
}
