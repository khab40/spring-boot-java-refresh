package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.AuthLoginRequest;
import com.example.springbootjavarefresh.dto.ApiKeyIssueResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.dto.MessageResponse;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private ApiKeysService apiKeysService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldRegisterAndRequireEmailVerification() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("auth@example.com");
        request.setFirstName("Auth");
        request.setLastName("User");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(1L);
        user.setEmail("auth@example.com");

        when(userService.createUser(request)).thenReturn(user);

        var response = authService.register(request);

        assertNull(response.accessToken());
        assertNull(response.apiKey());
        assertEquals(false, response.emailVerified());
    }

    @Test
    void shouldLoginAndReturnJwtPlusApiKey() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setEmail("auth@example.com");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(1L);
        user.setEmail("auth@example.com");
        user.setEmailVerified(Boolean.TRUE);

        when(userService.getUserByEmail("auth@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(apiKeysService.issueKeyForUser(user))
                .thenReturn(new ApiKeyIssueResponse(1L, "auth@example.com", "mdr_key", "mdr_key", null, null));

        var response = authService.login(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("mdr_key", response.apiKey());
    }

    @Test
    void shouldRejectLoginWhenEmailIsNotVerified() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setEmail("auth@example.com");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(1L);
        user.setEmail("auth@example.com");
        user.setEmailVerified(Boolean.FALSE);

        when(userService.getUserByEmail("auth@example.com")).thenReturn(java.util.Optional.of(user));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.login(request));

        assertEquals("Email verification required. Check your inbox before signing in.", exception.getMessage());
    }

    @Test
    void shouldVerifyEmail() {
        when(emailVerificationService.verifyEmail("token"))
                .thenReturn(new EmailVerificationResponse(true, "auth@example.com", "Email verified. You can now sign in.", null));

        var response = authService.verifyEmail("token");

        assertEquals(true, response.verified());
        assertEquals("auth@example.com", response.email());
    }

    @Test
    void shouldResendVerificationEmail() {
        User user = new User();
        user.setId(1L);
        user.setEmail("auth@example.com");
        user.setEmailVerified(Boolean.FALSE);

        when(userService.getUserByEmail("auth@example.com")).thenReturn(java.util.Optional.of(user));

        MessageResponse response = authService.resendVerificationEmail("auth@example.com");

        assertEquals("Verification email sent. Check your inbox.", response.message());
    }
}
