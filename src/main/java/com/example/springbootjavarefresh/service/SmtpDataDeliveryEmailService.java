package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.OtdDeliveryFileResponse;
import com.example.springbootjavarefresh.dto.OtdDeliveryResponse;
import com.example.springbootjavarefresh.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpDataDeliveryEmailService implements DataDeliveryEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpDataDeliveryEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String mailHost;

    public SmtpDataDeliveryEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@marketdatalake.local}") String fromAddress,
            @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailHost = mailHost;
    }

    @Override
    public void sendDeliveryEmail(User user, OtdDeliveryResponse deliveryResponse) {
        if (mailHost == null || mailHost.isBlank()) {
            logger.warn("Skipping delivery email for {} because SMTP is not configured.", user.getEmail());
            return;
        }

        String links = deliveryResponse.files().stream()
                .map(OtdDeliveryFileResponse::signedUrl)
                .reduce("", (left, right) -> left.isBlank() ? right : left + System.lineSeparator() + right);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("Market Data Lake delivery ready");
            message.setText("""
                    Your on-time delivery is ready.

                    Product: %s
                    Rows exported: %d
                    Files: %d
                    Consumed volume: %s MB

                    Download links:
                    %s
                    """.formatted(
                    deliveryResponse.productCode(),
                    deliveryResponse.rowCount(),
                    deliveryResponse.fileCount(),
                    deliveryResponse.consumedMegabytes(),
                    links
            ));
            mailSender.send(message);
        } catch (RuntimeException exception) {
            logger.warn("Failed to send data delivery email to {}", user.getEmail(), exception);
        }
    }
}
