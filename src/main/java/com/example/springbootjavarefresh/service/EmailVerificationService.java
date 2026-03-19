package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.EmailVerificationResponse;
import com.example.springbootjavarefresh.entity.EmailVerificationToken;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.EmailVerificationTokenRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final VerificationEmailService verificationEmailService;
    private final long expirationHours;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            VerificationEmailService verificationEmailService,
            @Value("${app.auth.verification-expiration-hours:24}") long expirationHours) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.verificationEmailService = verificationEmailService;
        this.expirationHours = expirationHours;
    }

    @Transactional
    public void sendVerificationForUser(User user) {
        expireActiveTokens(user);

        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        tokenRepository.save(token);

        verificationEmailService.sendVerificationEmail(user, rawToken);
    }

    @Transactional
    public EmailVerificationResponse verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Verification token is invalid."));

        if (token.getUsedAt() != null) {
            throw new IllegalStateException("Verification token has already been used.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification token has expired.");
        }

        User user = token.getUser();
        user.setEmailVerified(Boolean.TRUE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return new EmailVerificationResponse(
                true,
                user.getEmail(),
                "Email verified. You can now sign in.",
                user.getEmailVerifiedAt()
        );
    }

    private void expireActiveTokens(User user) {
        List<EmailVerificationToken> activeTokens = tokenRepository.findAllByUserAndUsedAtIsNull(user);
        LocalDateTime now = LocalDateTime.now();
        for (EmailVerificationToken activeToken : activeTokens) {
            activeToken.setUsedAt(now);
        }
        tokenRepository.saveAll(activeTokens);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
