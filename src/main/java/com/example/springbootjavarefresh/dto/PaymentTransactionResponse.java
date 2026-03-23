package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentTransactionResponse(
        Long id,
        BigDecimal amount,
        String currency,
        Object status,
        String checkoutUrl,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        DataProductResponse product,
        List<PaymentTransactionItemResponse> items) {

    public static PaymentTransactionResponse fromTransaction(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCheckoutUrl(),
                transaction.getErrorMessage(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getProduct() == null ? null : DataProductResponse.fromProduct(transaction.getProduct()),
                transaction.getItems() == null ? List.of() : transaction.getItems().stream()
                        .map(PaymentTransactionItemResponse::fromItem)
                        .toList()
        );
    }
}
