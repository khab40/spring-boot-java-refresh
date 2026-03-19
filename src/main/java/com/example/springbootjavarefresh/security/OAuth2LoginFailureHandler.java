package com.example.springbootjavarefresh.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendFailureUrl;

    public OAuth2LoginFailureHandler(
            @Value("${app.auth.oauth2.failure-url:http://localhost:3000/?authError=google-signin-failed}") String frontendFailureUrl) {
        this.frontendFailureUrl = frontendFailureUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String separator = frontendFailureUrl.contains("?") ? "&" : "?";
        response.sendRedirect(frontendFailureUrl + separator + "message=" + UriUtils.encode(exception.getMessage(), StandardCharsets.UTF_8));
    }
}
