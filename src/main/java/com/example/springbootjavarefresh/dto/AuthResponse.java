package com.example.springbootjavarefresh.dto;

public record AuthResponse(
        Long userId,
        String email,
        String accessToken,
        String tokenType,
        String apiKey) {
}
