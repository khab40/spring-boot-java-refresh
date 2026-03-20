package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.PaymentTransaction;

public interface StripePaymentGateway {

    CheckoutSessionData createCheckoutSession(PaymentTransaction transaction, String successUrl, String cancelUrl) throws Exception;

    CheckoutSessionStatus getCheckoutSessionStatus(String sessionId) throws Exception;

    record CheckoutSessionData(String sessionId, String checkoutUrl) {}

    record CheckoutSessionStatus(String sessionId, String status, String paymentStatus, String checkoutUrl) {}
}
