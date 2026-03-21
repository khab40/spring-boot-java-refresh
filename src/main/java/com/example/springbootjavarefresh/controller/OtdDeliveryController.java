package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.OtdDeliveryRequest;
import com.example.springbootjavarefresh.dto.OtdDeliveryResponse;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.service.OtdDeliveryService;
import com.example.springbootjavarefresh.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market-data/otd-deliveries")
@Tag(name = "OTD Deliveries", description = "On-time delivery query and file delivery workflow")
public class OtdDeliveryController {

    private final OtdDeliveryService otdDeliveryService;
    private final UserService userService;

    public OtdDeliveryController(OtdDeliveryService otdDeliveryService, UserService userService) {
        this.otdDeliveryService = otdDeliveryService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Run an OTD SQL query, export the result to FSS, and return signed download links")
    public ResponseEntity<OtdDeliveryResponse> createDelivery(
            Authentication authentication,
            @Valid @RequestBody OtdDeliveryRequest request) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(otdDeliveryService.createDelivery(user.getId(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the current user's OTD deliveries and signed download links")
    public ResponseEntity<List<OtdDeliveryResponse>> myDeliveries(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(otdDeliveryService.getDeliveriesForUser(user.getId()));
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        }
        throw new IllegalStateException("Unsupported authenticated principal");
    }
}
