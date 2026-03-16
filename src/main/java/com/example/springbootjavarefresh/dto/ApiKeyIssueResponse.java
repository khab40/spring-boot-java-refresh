package com.example.springbootjavarefresh.dto;

import java.time.LocalDateTime;

public record ApiKeyIssueResponse(
        Long userId,
        String email,
        String apiKey,
        String keyPrefix,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt) {
}
