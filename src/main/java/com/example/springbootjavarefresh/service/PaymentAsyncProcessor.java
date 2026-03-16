package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PaymentAsyncProcessor {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final StripePaymentGateway stripePaymentGateway;

    public PaymentAsyncProcessor(
            PaymentTransactionRepository paymentTransactionRepository,
            StripePaymentGateway stripePaymentGateway) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.stripePaymentGateway = stripePaymentGateway;
    }

    @Async
    public CompletableFuture<Void> createCheckoutSessionAsync(Long transactionId, String successUrl, String cancelUrl) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found: " + transactionId));

        try {
            StripePaymentGateway.CheckoutSessionData checkoutSession =
                    stripePaymentGateway.createCheckoutSession(transaction, successUrl, cancelUrl);

            transaction.setStripeCheckoutSessionId(checkoutSession.sessionId());
            transaction.setCheckoutUrl(checkoutSession.checkoutUrl());
            transaction.setStatus(PaymentTransactionStatus.CHECKOUT_CREATED);
            transaction.setErrorMessage(null);
        } catch (Exception ex) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setErrorMessage(ex.getMessage());
        }

        paymentTransactionRepository.save(transaction);
        return CompletableFuture.completedFuture(null);
    }
}
