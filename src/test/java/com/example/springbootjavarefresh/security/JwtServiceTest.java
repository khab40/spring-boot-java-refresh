package com.example.springbootjavarefresh.security;

import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void shouldGenerateAndParseTokenWithRawSecretContainingHyphens() {
        JwtService jwtService = new JwtService("change-me-to-a-long-random-secret-key-for-jwt-signing", 24);

        User user = new User();
        user.setId(7L);
        user.setEmail("jwt@example.com");
        user.setRole(UserRole.USER);

        String token = assertDoesNotThrow(() -> jwtService.generateToken(user));

        assertEquals("jwt@example.com", jwtService.extractUsername(token));
    }
}
