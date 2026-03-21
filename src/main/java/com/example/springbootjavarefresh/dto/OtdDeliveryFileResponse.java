package com.example.springbootjavarefresh.dto;

import java.time.LocalDateTime;

public record OtdDeliveryFileResponse(
        String fileName,
        String objectKey,
        String signedUrl,
        long sizeBytes,
        LocalDateTime linkExpiresAt) {
}
