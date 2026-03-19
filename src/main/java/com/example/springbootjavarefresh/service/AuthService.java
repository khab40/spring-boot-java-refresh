package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.AuthLoginRequest;
import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.dto.MessageResponse;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ApiKeysService apiKeysService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            ApiKeysService apiKeysService,
            EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.apiKeysService = apiKeysService;
        this.emailVerificationService = emailVerificationService;
    }

    public AuthResponse register(CreateUserRequest request) {
        User user = userService.createUser(request);
        emailVerificationService.sendVerificationForUser(user);
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                null,
                null,
                null,
                false,
                "Registration successful. Check your email for the verification link."
        );
    }

    public AuthResponse login(AuthLoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Email verification required. Check your inbox before signing in.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtService.generateToken(user);
        String apiKey = apiKeysService.issueKeyForUser(user).apiKey();
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                token,
                "Bearer",
                apiKey,
                true,
                "Signed in successfully."
        );
    }

    public EmailVerificationResponse verifyEmail(String token) {
        return emailVerificationService.verifyEmail(token);
    }

    public MessageResponse resendVerificationEmail(String email) {
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified. You can sign in.");
        }

        emailVerificationService.sendVerificationForUser(user);
        return new MessageResponse("Verification email sent. Check your inbox.");
    }
}
