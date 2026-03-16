package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.ApiKeyIssueResponse;
import com.example.springbootjavarefresh.dto.ApiKeyLoginRequest;
import com.example.springbootjavarefresh.dto.ApiKeyUsageRequest;
import com.example.springbootjavarefresh.dto.ApiKeyUsageSummaryResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.service.ApiKeysService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access")
@Tag(name = "API Access", description = "API key issuance and usage control endpoints")
public class ApiKeyController {

    private final ApiKeysService apiKeysService;

    public ApiKeyController(ApiKeysService apiKeysService) {
        this.apiKeysService = apiKeysService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user and issue a new API key")
    public ResponseEntity<ApiKeyIssueResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(apiKeysService.registerAndIssueKey(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Issue a new API key for an existing user login")
    public ResponseEntity<ApiKeyIssueResponse> login(@Valid @RequestBody ApiKeyLoginRequest request) {
        return ResponseEntity.ok(apiKeysService.loginAndIssueKey(request));
    }

    @PostMapping("/usage")
    @Operation(summary = "Record API key usage and enforce purchased limits")
    public ResponseEntity<ApiKeyUsageSummaryResponse> recordUsage(@Valid @RequestBody ApiKeyUsageRequest request) {
        return ResponseEntity.ok(apiKeysService.recordUsage(request));
    }

    @GetMapping("/usage/summary")
    @Operation(summary = "Get usage summary for an API key and data product")
    public ResponseEntity<ApiKeyUsageSummaryResponse> getUsageSummary(
            @RequestParam String apiKey,
            @RequestParam Long productId) {
        return ResponseEntity.ok(apiKeysService.getUsageSummary(apiKey, productId));
    }
}
