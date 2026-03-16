package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentSummaryResponse(
        Long id,
        Long userId,
        String userEmail,
        Long productId,
        String productCode,
        BigDecimal amount,
        String currency,
        PaymentTransactionStatus status,
        String checkoutUrl,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminPaymentSummaryResponse fromTransaction(PaymentTransaction transaction) {
        return new AdminPaymentSummaryResponse(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getUser().getEmail(),
                transaction.getProduct().getId(),
                transaction.getProduct().getCode(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCheckoutUrl(),
                transaction.getErrorMessage(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
