package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.entity.EmailVerificationToken;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.EmailVerificationTokenRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationEmailService verificationEmailService;

    private EmailVerificationService emailVerificationServiceManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailVerificationServiceManager = new EmailVerificationService(
                tokenRepository,
                userRepository,
                verificationEmailService,
                24
        );
    }

    @Test
    void shouldCreateTokenAndSendEmail() {
        User user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");

        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationServiceManager.sendVerificationForUser(user);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(verificationEmailService).sendVerificationEmail(any(User.class), any(String.class));
        assertEquals("buyer@example.com", tokenCaptor.getValue().getUser().getEmail());
        assertTrue(tokenCaptor.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldVerifyEmail() {
        User user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setEmailVerified(Boolean.FALSE);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationResponse response = emailVerificationServiceManager.verifyEmail("raw-token");

        assertTrue(response.verified());
        assertTrue(user.isEmailVerified());
        assertTrue(token.getUsedAt() != null);
    }
}
