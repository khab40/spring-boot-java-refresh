package com.example.springbootjavarefresh.dto;

public record MarketDataRuntimeStatusResponse(
        String mode,
        boolean stubbed,
        String message
) {
}
