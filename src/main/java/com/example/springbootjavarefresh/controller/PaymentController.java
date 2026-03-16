package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.PaymentCheckoutRequest;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.service.PaymentService;
import com.example.springbootjavarefresh.service.PaymentWebhookService;
import com.stripe.exception.SignatureVerificationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "API for asynchronous Stripe payment flows")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentController(PaymentService paymentService, PaymentWebhookService paymentWebhookService) {
        this.paymentService = paymentService;
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create an asynchronous Stripe checkout request")
    public ResponseEntity<PaymentTransaction> createCheckout(@Valid @RequestBody PaymentCheckoutRequest request) {
        PaymentTransaction transaction = paymentService.initiateCheckout(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transaction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment transaction status")
    public ResponseEntity<PaymentTransaction> getPaymentTransaction(@PathVariable Long id) {
        return paymentService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/webhook")
    @Operation(summary = "Handle Stripe webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload) throws SignatureVerificationException {
        paymentWebhookService.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed");
    }
}
