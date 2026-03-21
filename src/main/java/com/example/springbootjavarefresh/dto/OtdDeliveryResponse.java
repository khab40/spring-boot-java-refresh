package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.DataDeliveryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OtdDeliveryResponse(
        Long deliveryId,
        Long productId,
        String productCode,
        String productName,
        DataDeliveryStatus status,
        String sqlText,
        int rowCount,
        int fileCount,
        long totalBytes,
        BigDecimal consumedMegabytes,
        BigDecimal remainingBatchMegabytes,
        LocalDateTime createdAt,
        List<OtdDeliveryFileResponse> files) {
}
