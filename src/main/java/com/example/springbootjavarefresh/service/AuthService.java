package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.AuthLoginRequest;
import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
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

    public AuthService(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            ApiKeysService apiKeysService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.apiKeysService = apiKeysService;
    }

    public AuthResponse register(CreateUserRequest request) {
        User user = userService.createUser(request);
        String token = jwtService.generateToken(user);
        String apiKey = apiKeysService.issueKeyForUser(user).apiKey();
        return new AuthResponse(user.getId(), user.getEmail(), token, "Bearer", apiKey);
    }

    public AuthResponse login(AuthLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userService.getUserByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + request.getEmail()));
        String token = jwtService.generateToken(user);
        String apiKey = apiKeysService.issueKeyForUser(user).apiKey();
        return new AuthResponse(user.getId(), user.getEmail(), token, "Bearer", apiKey);
    }
}
