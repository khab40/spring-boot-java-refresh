package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.PaymentTransactionItem;

import java.math.BigDecimal;

public record PaymentTransactionItemResponse(
        Long id,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount,
        String currency,
        DataProductResponse product) {

    public static PaymentTransactionItemResponse fromItem(PaymentTransactionItem item) {
        return new PaymentTransactionItemResponse(
                item.getId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineAmount(),
                item.getCurrency(),
                DataProductResponse.fromProduct(item.getProduct())
        );
    }
}
