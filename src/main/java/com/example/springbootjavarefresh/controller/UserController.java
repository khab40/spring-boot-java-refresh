package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.service.UserEntitlementService;
import com.example.springbootjavarefresh.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/{id}/entitlements")
    @Operation(summary = "Get a user's entitlements")
    public ResponseEntity<List<UserEntitlement>> getEntitlements(@PathVariable Long id) {
        return ResponseEntity.ok(userEntitlementService.getEntitlementsByUserId(id));
    }
}
