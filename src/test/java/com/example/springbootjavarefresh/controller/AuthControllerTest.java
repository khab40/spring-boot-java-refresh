package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.AuthResponse;
import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.dto.UserProfileResponse;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.AuthService;
import com.example.springbootjavarefresh.service.PaymentService;
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
    private PaymentService paymentService;

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
                .thenReturn(new AuthResponse(1L, "auth@example.com", null, null, null, false, "Check your email"));

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
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.message").value("Check your email"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        when(authService.login(any()))
                .thenReturn(new AuthResponse(1L, "auth@example.com", "jwt-token", "Bearer", "mdr_key", true, "Signed in successfully."));

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
    void shouldVerifyEmail() throws Exception {
        when(authService.verifyEmail("token"))
                .thenReturn(new EmailVerificationResponse(true, "auth@example.com", "Email verified. You can now sign in.", null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/verify-email")
                        .param("token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.email").value("auth@example.com"));
    }

    @Test
    void shouldResendVerificationEmail() throws Exception {
        when(authService.resendVerificationEmail("auth@example.com"))
                .thenReturn(new com.example.springbootjavarefresh.dto.MessageResponse("Verification email sent. Check your inbox."));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "auth@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email sent. Check your inbox."));
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
    void shouldReturnCurrentUserPayments() {
        User userPrincipal = new User();
        userPrincipal.setId(1L);
        userPrincipal.setEmail("auth@example.com");
        userPrincipal.setFirstName("Auth");
        userPrincipal.setLastName("User");
        userPrincipal.setPasswordHash("ignored");

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(42L);
        transaction.setStatus(PaymentTransactionStatus.CHECKOUT_CREATED);

        when(paymentService.getTransactionsByUserId(1L)).thenReturn(List.of(transaction));

        ResponseEntity<List<PaymentTransaction>> response = authController.myPayments(new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        ));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(42L, response.getBody().get(0).getId());
    }

    @Test
    void shouldLogoutWithNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
