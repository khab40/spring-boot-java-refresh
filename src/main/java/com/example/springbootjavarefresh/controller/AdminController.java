package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AdminDashboardResponse;
import com.example.springbootjavarefresh.dto.AdminPaymentSummaryResponse;
import com.example.springbootjavarefresh.dto.AdminUsageSummaryResponse;
import com.example.springbootjavarefresh.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Administrative control and audit APIs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminAuditService adminAuditService;

    public AdminController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get administrative dashboard data")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminAuditService.getDashboard());
    }

    @GetMapping("/payments")
    @Operation(summary = "Get recent payment activity")
    public ResponseEntity<List<AdminPaymentSummaryResponse>> getRecentPayments() {
        return ResponseEntity.ok(adminAuditService.getRecentPayments());
    }

    @GetMapping("/usage")
    @Operation(summary = "Get recent usage activity")
    public ResponseEntity<List<AdminUsageSummaryResponse>> getRecentUsage() {
        return ResponseEntity.ok(adminAuditService.getRecentUsage());
    }
}
