package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.PaymentTransaction;

public interface StripePaymentGateway {

    CheckoutSessionData createCheckoutSession(PaymentTransaction transaction, String successUrl, String cancelUrl) throws Exception;

    record CheckoutSessionData(String sessionId, String checkoutUrl) {}
}
