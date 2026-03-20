package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CatalogItemResponse(
        Long id,
        String code,
        String name,
        String summary,
        String description,
        String marketDataType,
        String storageSystem,
        String deliveryApiPath,
        String lakeQueryReference,
        String sampleSymbols,
        LocalDate coverageStartDate,
        LocalDate coverageEndDate,
        Boolean active,
        LocalDateTime createdAt,
        List<DataProduct> offers
) {
    public static CatalogItemResponse from(DataCatalogItem item, List<DataProduct> offers) {
        return new CatalogItemResponse(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getSummary(),
                item.getDescription(),
                item.getMarketDataType().name(),
                item.getStorageSystem().name(),
                item.getDeliveryApiPath(),
                item.getLakeQueryReference(),
                item.getSampleSymbols(),
                item.getCoverageStartDate(),
                item.getCoverageEndDate(),
                item.getActive(),
                item.getCreatedAt(),
                offers
        );
    }
}
