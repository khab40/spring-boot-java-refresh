package com.example.springbootjavarefresh.dto;

import java.time.LocalDateTime;

public record EmailVerificationResponse(
        boolean verified,
        String email,
        String message,
        LocalDateTime verifiedAt) {
}
