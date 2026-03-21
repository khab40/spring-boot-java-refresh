package com.example.springbootjavarefresh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtdDeliveryRequest(
        @NotNull Long productId,
        @NotBlank String sql) {
}
