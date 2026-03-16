package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StripePaymentGatewayImpl implements StripePaymentGateway {

    private final String stripeApiKey;

    public StripePaymentGatewayImpl(@Value("${stripe.api-key:}") String stripeApiKey) {
        this.stripeApiKey = stripeApiKey;
    }

    @Override
    public CheckoutSessionData createCheckoutSession(PaymentTransaction transaction, String successUrl, String cancelUrl) throws StripeException {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new IllegalStateException("Stripe API key is not configured");
        }

        Stripe.apiKey = stripeApiKey;

        SessionCreateParams.LineItem.PriceData.Builder priceDataBuilder = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(transaction.getCurrency().toLowerCase())
                .setUnitAmount(toMinorUnits(transaction.getAmount()))
                .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(transaction.getProduct().getName())
                                .setDescription(transaction.getProduct().getDescription())
                                .build()
                );

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setClientReferenceId(transaction.getId().toString())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("transactionId", transaction.getId().toString());

        if (transaction.getProduct().getAccessType() == ProductAccessType.SUBSCRIPTION) {
            SessionCreateParams.LineItem.PriceData.Recurring recurring = buildRecurring(transaction);
            priceDataBuilder.setRecurring(recurring);
            sessionBuilder.setMode(SessionCreateParams.Mode.SUBSCRIPTION);
        } else {
            sessionBuilder.setMode(SessionCreateParams.Mode.PAYMENT);
        }

        sessionBuilder.addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceDataBuilder.build())
                        .build()
        );

        Session session = Session.create(sessionBuilder.build());
        return new CheckoutSessionData(session.getId(), session.getUrl());
    }

    private SessionCreateParams.LineItem.PriceData.Recurring buildRecurring(PaymentTransaction transaction) {
        BillingInterval interval = transaction.getProduct().getBillingInterval();
        SessionCreateParams.LineItem.PriceData.Recurring.Interval stripeInterval =
                interval == BillingInterval.YEARLY
                        ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                        : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH;

        return SessionCreateParams.LineItem.PriceData.Recurring.builder()
                .setInterval(stripeInterval)
                .build();
    }

    private Long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
