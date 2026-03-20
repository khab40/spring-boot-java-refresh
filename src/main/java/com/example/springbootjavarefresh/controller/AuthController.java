package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AuthLoginRequest;
import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.EmailRequest;
import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.dto.MessageResponse;
import com.example.springbootjavarefresh.dto.UpdateUserProfileRequest;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.service.AuthService;
import com.example.springbootjavarefresh.service.PaymentService;
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
    private final PaymentService paymentService;
    private final UserEntitlementService userEntitlementService;
    private final UserService userService;

    public AuthController(
            AuthService authService,
            PaymentService paymentService,
            UserEntitlementService userEntitlementService,
            UserService userService) {
        this.authService = authService;
        this.paymentService = paymentService;
        this.userEntitlementService = userEntitlementService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user, persist them as unverified, and send a verification email")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and return a JWT plus API key")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify an email address using the registration token")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend the email verification link for an existing unverified user")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.resendVerificationEmail(request.email()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user profile")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(UserProfileResponse.fromUser(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the currently authenticated user profile")
    public ResponseEntity<UserProfileResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(UserProfileResponse.fromUser(userService.updateUserProfile(user.getId(), request)));
    }

    @GetMapping("/me/entitlements")
    @Operation(summary = "Get the currently authenticated user's entitlements")
    public ResponseEntity<List<UserEntitlement>> myEntitlements(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(userEntitlementService.getEntitlementsByUserId(user.getId()));
    }

    @GetMapping("/me/payments")
    @Operation(summary = "Get the currently authenticated user's payments")
    public ResponseEntity<List<PaymentTransaction>> myPayments(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(paymentService.getTransactionsByUserId(user.getId()));
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
