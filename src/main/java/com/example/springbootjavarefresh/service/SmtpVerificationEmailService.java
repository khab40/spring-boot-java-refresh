package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpVerificationEmailService implements VerificationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpVerificationEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String verificationBaseUrl;
    private final String mailHost;

    public SmtpVerificationEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@marketdatalake.local}") String fromAddress,
            @Value("${app.auth.verification-base-url:http://localhost:8080/api/auth/verify-email}") String verificationBaseUrl,
            @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.verificationBaseUrl = verificationBaseUrl;
        this.mailHost = mailHost;
    }

    @Override
    public void sendVerificationEmail(User user, String verificationToken) {
        if (mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException("Email delivery is not configured. Set SPRING_MAIL_HOST and related mail settings.");
        }

        String verificationUrl = verificationBaseUrl + "?token=" + verificationToken;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("Verify your Market Data Lake account");
            message.setText("""
                    Welcome to Market Data Lake.

                    Verify your email address by opening this link:
                    %s

                    If you did not create this account, you can ignore this email.
                    """.formatted(verificationUrl));
            mailSender.send(message);
        } catch (RuntimeException exception) {
            logger.error("Failed to send verification email to {}", user.getEmail(), exception);
            throw new IllegalStateException("Registration created the user, but verification email delivery failed.");
        }
    }
}
