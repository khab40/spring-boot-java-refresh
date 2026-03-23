package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.AuthLoginRequest;
import com.example.springbootjavarefresh.dto.ApiKeyIssueResponse;
import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.dto.MessageResponse;
import com.example.springbootjavarefresh.entity.AuthProvider;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.List;
import java.util.Map;

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
        assertEquals("auth@example.com", response.profile().email());
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
        assertEquals("auth@example.com", response.profile().email());
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
    void shouldRejectPasswordLoginForGoogleAccount() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setEmail("auth@example.com");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(1L);
        user.setEmail("auth@example.com");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(Boolean.TRUE);

        when(userService.getUserByEmail("auth@example.com")).thenReturn(java.util.Optional.of(user));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.login(request));

        assertEquals("This account uses Google sign-in. Continue with Google.", exception.getMessage());
    }

    @Test
    void shouldAllowPasswordLoginForGoogleLinkedAccountWhenPasswordExists() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setEmail("auth@example.com");
        request.setPassword("super-secret");

        User user = new User();
        user.setId(1L);
        user.setEmail("auth@example.com");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setPasswordHash("$2a$10$existing-hash");
        user.setEmailVerified(Boolean.TRUE);

        when(userService.getUserByEmail("auth@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(apiKeysService.issueKeyForUser(user))
                .thenReturn(new ApiKeyIssueResponse(1L, "auth@example.com", "mdr_key", "mdr_key", null, null));

        var response = authService.login(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("mdr_key", response.apiKey());
        assertEquals("auth@example.com", response.profile().email());
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

    @Test
    void shouldLoginWithGoogleAndReturnJwtPlusApiKey() {
        User user = new User();
        user.setId(7L);
        user.setEmail("google@example.com");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(Boolean.TRUE);

        DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(Map.of(
                        "sub", "google-subject",
                        "email", "google@example.com",
                        "given_name", "Go",
                        "family_name", "Ogler"
                ))),
                Map.of(
                        "sub", "google-subject",
                        "email", "google@example.com",
                        "given_name", "Go",
                        "family_name", "Ogler"
                ),
                "sub"
        );

        when(userService.upsertGoogleUser("google@example.com", "Go", "Ogler", "google-subject")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(apiKeysService.issueKeyForUser(user))
                .thenReturn(new ApiKeyIssueResponse(7L, "google@example.com", "mdr_key", "mdr_key", null, null));

        var response = authService.loginWithGoogle(oauth2User);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("mdr_key", response.apiKey());
        assertEquals(true, response.emailVerified());
        assertEquals("google@example.com", response.profile().email());
    }
}
