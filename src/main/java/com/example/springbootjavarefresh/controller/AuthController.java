package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AuthLoginRequest;
import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.service.AuthService;
import com.example.springbootjavarefresh.service.UserEntitlementService;
import com.example.springbootjavarefresh.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Credential-based registration and login")
public class AuthController {

    private final AuthService authService;
    private final UserEntitlementService userEntitlementService;
    private final UserService userService;

    public AuthController(
            AuthService authService,
            UserEntitlementService userEntitlementService,
            UserService userService) {
        this.authService = authService;
        this.userEntitlementService = userEntitlementService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user with password credentials and return a JWT plus API key")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and return a JWT plus API key")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user profile")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(UserProfileResponse.fromUser(user));
    }

    @GetMapping("/me/entitlements")
    @Operation(summary = "Get the currently authenticated user's entitlements")
    public ResponseEntity<List<UserEntitlement>> myEntitlements(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(userEntitlementService.getEntitlementsByUserId(user.getId()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out the current session on the client")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
