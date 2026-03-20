package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionItem;
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
        configureStripe();

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setClientReferenceId(transaction.getId().toString())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("transactionId", transaction.getId().toString());

        if (transaction.getProduct().getAccessType() == ProductAccessType.SUBSCRIPTION) {
            sessionBuilder.setMode(SessionCreateParams.Mode.SUBSCRIPTION);
        } else {
            sessionBuilder.setMode(SessionCreateParams.Mode.PAYMENT);
        }

        java.util.List<PaymentTransactionItem> lineItems = transaction.getItems();
        if (lineItems == null || lineItems.isEmpty()) {
            PaymentTransactionItem legacyItem = new PaymentTransactionItem();
            legacyItem.setProduct(transaction.getProduct());
            legacyItem.setQuantity(1);
            legacyItem.setUnitPrice(transaction.getAmount());
            legacyItem.setLineAmount(transaction.getAmount());
            legacyItem.setCurrency(transaction.getCurrency());
            lineItems = java.util.List.of(legacyItem);
        }

        for (PaymentTransactionItem item : lineItems) {
            SessionCreateParams.LineItem.PriceData.Builder priceDataBuilder = SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(item.getCurrency().toLowerCase())
                    .setUnitAmount(toMinorUnits(item.getUnitPrice()))
                    .setProductData(
                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getProduct().getName())
                                    .setDescription(item.getProduct().getDescription())
                                    .build()
                    );

            if (item.getProduct().getAccessType() == ProductAccessType.SUBSCRIPTION) {
                priceDataBuilder.setRecurring(buildRecurring(item));
            }

            sessionBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(item.getQuantity().longValue())
                            .setPriceData(priceDataBuilder.build())
                            .build()
            );
        }

        Session session = Session.create(sessionBuilder.build());
        return new CheckoutSessionData(session.getId(), session.getUrl());
    }

    @Override
    public CheckoutSessionStatus getCheckoutSessionStatus(String sessionId) throws StripeException {
        configureStripe();
        Session session = Session.retrieve(sessionId);
        return new CheckoutSessionStatus(session.getId(), session.getStatus(), session.getPaymentStatus(), session.getUrl());
    }

    private SessionCreateParams.LineItem.PriceData.Recurring buildRecurring(PaymentTransactionItem item) {
        BillingInterval interval = item.getProduct().getBillingInterval();
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

    private void configureStripe() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new IllegalStateException("Stripe API key is not configured");
        }
        Stripe.apiKey = stripeApiKey;
    }
}
