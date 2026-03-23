package com.example.springbootjavarefresh.security;

import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendSuccessUrl;
    private final ObjectMapper objectMapper;

    public OAuth2LoginSuccessHandler(
            @Lazy AuthService authService,
            ObjectMapper objectMapper,
            @Value("${app.auth.oauth2.success-url:http://localhost:3000/oauth/callback}") String frontendSuccessUrl) {
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.frontendSuccessUrl = frontendSuccessUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        AuthResponse authResponse = authService.loginWithGoogle(oauth2User);

        String redirectUrl = frontendSuccessUrl + "#" +
                "accessToken=" + encode(authResponse.accessToken()) +
                "&apiKey=" + encode(authResponse.apiKey()) +
                "&userId=" + authResponse.userId() +
                "&email=" + encode(authResponse.email()) +
                "&profile=" + encode(objectMapper.writeValueAsString(authResponse.profile())) +
                "&message=" + encode(authResponse.message());

        response.sendRedirect(redirectUrl);
    }

    private String encode(String value) {
        return UriUtils.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
