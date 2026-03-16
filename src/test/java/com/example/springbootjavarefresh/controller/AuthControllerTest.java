package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.AuthService;
import com.example.springbootjavarefresh.service.UserEntitlementService;
import com.example.springbootjavarefresh.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthController authController;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserEntitlementService userEntitlementService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldRegisterUser() throws Exception {
        when(authService.register(any()))
                .thenReturn(new AuthResponse(1L, "auth@example.com", "jwt-token", "Bearer", "mdr_key"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "auth@example.com",
                                  "firstName": "Auth",
                                  "lastName": "User",
                                  "password": "super-secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.apiKey").value("mdr_key"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        when(authService.login(any()))
                .thenReturn(new AuthResponse(1L, "auth@example.com", "jwt-token", "Bearer", "mdr_key"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "auth@example.com",
                                  "password": "super-secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void shouldReturnCurrentUserProfile() {
        User userPrincipal = new User();
        userPrincipal.setId(1L);
        userPrincipal.setEmail("auth@example.com");
        userPrincipal.setFirstName("Auth");
        userPrincipal.setLastName("User");
        userPrincipal.setPasswordHash("ignored");
        when(userService.getUserByEmail("auth@example.com")).thenReturn(Optional.of(userPrincipal));

        ResponseEntity<UserProfileResponse> response = authController.me(new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        ));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("auth@example.com", response.getBody().email());
    }

    @Test
    void shouldReturnCurrentUserEntitlements() throws Exception {
        User userPrincipal = new User();
        userPrincipal.setId(1L);
        userPrincipal.setEmail("auth@example.com");
        userPrincipal.setFirstName("Auth");
        userPrincipal.setLastName("User");
        userPrincipal.setPasswordHash("ignored");

        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(99L);
        entitlement.setUser(userPrincipal);

        when(userService.getUserByEmail("auth@example.com")).thenReturn(Optional.of(userPrincipal));
        when(userEntitlementService.getEntitlementsByUserId(1L)).thenReturn(List.of(entitlement));

        ResponseEntity<List<UserEntitlement>> response = authController.myEntitlements(new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        ));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(99L, response.getBody().get(0).getId());
    }

    @Test
    void shouldLogoutWithNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
