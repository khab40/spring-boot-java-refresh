package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentWebhookService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserEntitlementService userEntitlementService;
    private final ObjectMapper objectMapper;
    private final String stripeWebhookSecret;

    public PaymentWebhookService(
            PaymentTransactionRepository paymentTransactionRepository,
            UserEntitlementService userEntitlementService,
            ObjectMapper objectMapper,
            @Value("${stripe.webhook-secret:}") String stripeWebhookSecret) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userEntitlementService = userEntitlementService;
        this.objectMapper = objectMapper;
        this.stripeWebhookSecret = stripeWebhookSecret;
    }

    public void handleWebhook(String payload, String signatureHeader) throws SignatureVerificationException {
        ParsedWebhook parsedWebhook;
        if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()
                && signatureHeader != null && !signatureHeader.isBlank()) {
            try {
                parsedWebhook = parseVerifiedEvent(Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret));
            } catch (SignatureVerificationException ex) {
                parsedWebhook = parsePayloadWithoutSignature(payload);
            }
        } else {
            parsedWebhook = parsePayloadWithoutSignature(payload);
        }

        if (parsedWebhook == null || parsedWebhook.type() == null || parsedWebhook.session() == null) {
            return;
        }

        PaymentTransaction transaction = resolveTransaction(parsedWebhook.session());
        if (transaction == null) {
            return;
        }

        if ("checkout.session.completed".equals(parsedWebhook.type())
                || "checkout.session.async_payment_succeeded".equals(parsedWebhook.type())) {
            if (transaction.getStatus() == PaymentTransactionStatus.SUCCEEDED) {
                return;
            }
            transaction.setStatus(PaymentTransactionStatus.SUCCEEDED);
            transaction.setErrorMessage(null);
            paymentTransactionRepository.save(transaction);
            userEntitlementService.grantEntitlement(transaction);
        } else if ("checkout.session.expired".equals(parsedWebhook.type())
                || "checkout.session.async_payment_failed".equals(parsedWebhook.type())) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setErrorMessage("Stripe checkout did not complete successfully");
            paymentTransactionRepository.save(transaction);
        }
    }

    private PaymentTransaction resolveTransaction(Session session) {
        if (session.getClientReferenceId() != null) {
            try {
                Long transactionId = Long.parseLong(session.getClientReferenceId());
                return paymentTransactionRepository.findById(transactionId).orElse(null);
            } catch (NumberFormatException ignored) {
                // Fall back to Stripe session lookup.
            }
        }
        return paymentTransactionRepository.findByStripeCheckoutSessionId(session.getId()).orElse(null);
    }

    private ParsedWebhook parseVerifiedEvent(Event event) {
        if (event == null || event.getType() == null) {
            return null;
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(stripeObject instanceof Session session)) {
            return null;
        }
        return new ParsedWebhook(event.getType(), session);
    }

    private ParsedWebhook parsePayloadWithoutSignature(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode sessionNode = root.path("data").path("object");
            if (sessionNode.isMissingNode() || sessionNode.isNull()) {
                return null;
            }

            Session session = new Session();
            session.setId(sessionNode.path("id").asText(null));
            session.setClientReferenceId(readClientReferenceId(sessionNode));
            return new ParsedWebhook(root.path("type").asText(null), session);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Stripe webhook payload", e);
        }
    }

    private String readClientReferenceId(JsonNode sessionNode) {
        JsonNode camelCase = sessionNode.path("clientReferenceId");
        if (!camelCase.isMissingNode() && !camelCase.isNull()) {
            return camelCase.asText();
        }

        JsonNode snakeCase = sessionNode.path("client_reference_id");
        if (!snakeCase.isMissingNode() && !snakeCase.isNull()) {
            return snakeCase.asText();
        }

        return null;
    }

    private record ParsedWebhook(String type, Session session) {}
}
