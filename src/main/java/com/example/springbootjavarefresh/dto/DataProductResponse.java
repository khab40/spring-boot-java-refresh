package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.DataProduct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DataProductResponse(
        Long id,
        Long catalogItemId,
        String code,
        String name,
        String description,
        BigDecimal price,
        BigDecimal minimumPrice,
        Integer includedSymbols,
        Integer includedDays,
        BigDecimal pricePerAdditionalSymbol,
        BigDecimal pricePerAdditionalDay,
        Integer fullUniverseSymbolCount,
        String currency,
        Object accessType,
        Object billingInterval,
        BigDecimal batchDownloadLimitMb,
        Integer realtimeSubscriptionLimit,
        Integer maxRealtimePayloadKb,
        BigDecimal quotedPrice,
        Integer quotedSymbolCount,
        Integer quotedDayCount,
        LocalDate quotedStartDate,
        LocalDate quotedEndDate,
        String quotedSelectionSummary,
        String quotedPricingSummary,
        Boolean active,
        LocalDateTime createdAt) {

    public static DataProductResponse fromProduct(DataProduct product) {
        return new DataProductResponse(
                product.getId(),
                product.getCatalogItemId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getMinimumPrice(),
                product.getIncludedSymbols(),
                product.getIncludedDays(),
                product.getPricePerAdditionalSymbol(),
                product.getPricePerAdditionalDay(),
                product.getFullUniverseSymbolCount(),
                product.getCurrency(),
                product.getAccessType(),
                product.getBillingInterval(),
                product.getBatchDownloadLimitMb(),
                product.getRealtimeSubscriptionLimit(),
                product.getMaxRealtimePayloadKb(),
                product.getQuotedPrice(),
                product.getQuotedSymbolCount(),
                product.getQuotedDayCount(),
                product.getQuotedStartDate(),
                product.getQuotedEndDate(),
                product.getQuotedSelectionSummary(),
                product.getQuotedPricingSummary(),
                product.getActive(),
                product.getCreatedAt()
        );
    }
}
