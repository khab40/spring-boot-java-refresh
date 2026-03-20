package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
    List<PaymentTransaction> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
