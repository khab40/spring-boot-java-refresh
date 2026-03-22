package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AdminUpdateUserRequest;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.UpdateUserRoleRequest;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.service.UserEntitlementService;
import com.example.springbootjavarefresh.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "API for managing users and their entitlements")
public class UserController {

    private final UserService userService;
    private final UserEntitlementService userEntitlementService;

    public UserController(UserService userService, UserEntitlementService userEntitlementService) {
        this.userService = userService;
        this.userEntitlementService = userEntitlementService;
    }

    @GetMapping
    @Operation(summary = "Get all users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream().map(UserProfileResponse::fromUser).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserProfileResponse::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(UserProfileResponse.fromUser(userService.createUser(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin update for a user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(UserProfileResponse.fromUser(userService.updateUserAdmin(id, request)));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update a user's role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(UserProfileResponse.fromUser(userService.updateUserRole(id, request.getRole())));
    }

    @GetMapping("/{id}/entitlements")
    @Operation(summary = "Get a user's entitlements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntitlement>> getEntitlements(@PathVariable Long id) {
        return ResponseEntity.ok(userEntitlementService.getEntitlementsByUserId(id));
    }
}
