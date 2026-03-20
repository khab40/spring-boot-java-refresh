package com.example.springbootjavarefresh.observability;

public record ServiceStatus(
        String service,
        boolean up,
        String target,
        int httpStatus,
        String detail) {
}
