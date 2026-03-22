package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.PaymentCheckoutRequest;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionItem;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final DataProductRepository dataProductRepository;
    private final PaymentAsyncProcessor paymentAsyncProcessor;
    private final StripePaymentGateway stripePaymentGateway;
    private final UserEntitlementService userEntitlementService;
    private final CatalogPricingService catalogPricingService;

    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository,
            DataProductRepository dataProductRepository,
            PaymentAsyncProcessor paymentAsyncProcessor,
            StripePaymentGateway stripePaymentGateway,
            UserEntitlementService userEntitlementService,
            CatalogPricingService catalogPricingService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.dataProductRepository = dataProductRepository;
        this.paymentAsyncProcessor = paymentAsyncProcessor;
        this.stripePaymentGateway = stripePaymentGateway;
        this.userEntitlementService = userEntitlementService;
        this.catalogPricingService = catalogPricingService;
    }

    public PaymentTransaction initiateCheckout(PaymentCheckoutRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
        List<ResolvedCheckoutItem> resolvedItems = resolveCheckoutItems(request);
        validateCart(resolvedItems);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(user);
        transaction.setProduct(resolvedItems.get(0).product());
        transaction.setAmount(resolvedItems.stream()
                .map(ResolvedCheckoutItem::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        transaction.setCurrency(resolvedItems.get(0).product().getCurrency());
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        transaction.setItems(buildTransactionItems(transaction, resolvedItems));

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        paymentAsyncProcessor.createCheckoutSessionAsync(saved.getId(), request.getSuccessUrl(), request.getCancelUrl());
        return saved;
    }

    public Optional<PaymentTransaction> getTransactionById(Long id) {
        return paymentTransactionRepository.findById(id)
                .map(this::refreshTransactionStatusFromStripe);
    }

    public List<PaymentTransaction> getTransactionsByUserId(Long userId) {
        return paymentTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::refreshTransactionStatusFromStripe)
                .toList();
    }

    public PaymentTransaction refreshTransactionStatusFromStripe(PaymentTransaction transaction) {
        if (transaction.getStatus() != PaymentTransactionStatus.PENDING
                && transaction.getStatus() != PaymentTransactionStatus.CHECKOUT_CREATED) {
            return transaction;
        }
        if (transaction.getStripeCheckoutSessionId() == null || transaction.getStripeCheckoutSessionId().isBlank()) {
            return transaction;
        }

        try {
            StripePaymentGateway.CheckoutSessionStatus status =
                    stripePaymentGateway.getCheckoutSessionStatus(transaction.getStripeCheckoutSessionId());
            String sessionStatus = status.status() == null ? "" : status.status().toLowerCase();
            String paymentStatus = status.paymentStatus() == null ? "" : status.paymentStatus().toLowerCase();

            if ("complete".equals(sessionStatus) && ("paid".equals(paymentStatus) || "no_payment_required".equals(paymentStatus))) {
                if (transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
                    transaction.setStatus(PaymentTransactionStatus.SUCCEEDED);
                    transaction.setErrorMessage(null);
                    transaction.setCheckoutUrl(status.checkoutUrl());
                    paymentTransactionRepository.save(transaction);
                    userEntitlementService.grantEntitlement(transaction);
                }
            } else if ("expired".equals(sessionStatus)) {
                transaction.setStatus(PaymentTransactionStatus.FAILED);
                transaction.setErrorMessage("Stripe checkout expired");
                paymentTransactionRepository.save(transaction);
            } else if (status.checkoutUrl() != null && transaction.getCheckoutUrl() == null) {
                transaction.setCheckoutUrl(status.checkoutUrl());
                paymentTransactionRepository.save(transaction);
            }
        } catch (Exception exception) {
            log.warn("Unable to refresh Stripe status for transaction {}", transaction.getId(), exception);
            if (transaction.getErrorMessage() == null || transaction.getErrorMessage().isBlank()) {
                transaction.setErrorMessage("Unable to refresh Stripe status. Verify STRIPE_API_KEY and webhook delivery.");
                paymentTransactionRepository.save(transaction);
            }
            return transaction;
        }

        return transaction;
    }

    private List<ResolvedCheckoutItem> resolveCheckoutItems(PaymentCheckoutRequest request) {
        List<PaymentCheckoutRequest.CheckoutLineItem> requestItems = request.getItems();
        if (requestItems == null || requestItems.isEmpty()) {
            if (request.getProductId() == null) {
                throw new IllegalArgumentException("Checkout requires at least one offer");
            }
            PaymentCheckoutRequest.CheckoutLineItem item = new PaymentCheckoutRequest.CheckoutLineItem();
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity() == null ? 1 : request.getQuantity());
            requestItems = List.of(item);
        }

        List<ResolvedCheckoutItem> resolvedItems = new ArrayList<>();
        for (PaymentCheckoutRequest.CheckoutLineItem requestItem : requestItems) {
            int quantity = requestItem.getQuantity() == null ? 1 : requestItem.getQuantity();
            if (quantity < 1) {
                throw new IllegalArgumentException("Checkout quantities must be at least 1");
            }
            DataProduct product = dataProductRepository.findById(requestItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Data product not found: " + requestItem.getProductId()));
            DataCatalogItem catalogItem = product.getCatalogItem();
            if (catalogItem == null) {
                throw new IllegalArgumentException("Catalog item not found for product: " + product.getId());
            }
            BigDecimal unitPrice = catalogPricingService.quote(catalogItem, product, requestItem.getSelection()).quotedPrice();
            resolvedItems.add(new ResolvedCheckoutItem(
                    product,
                    quantity,
                    unitPrice,
                    unitPrice.multiply(BigDecimal.valueOf(quantity))
            ));
        }
        return resolvedItems;
    }

    private void validateCart(List<ResolvedCheckoutItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Checkout requires at least one offer");
        }

        DataProduct firstProduct = items.get(0).product();
        for (ResolvedCheckoutItem item : items) {
            DataProduct product = item.product();
            if (!firstProduct.getCurrency().equalsIgnoreCase(product.getCurrency())) {
                throw new IllegalArgumentException("Stripe checkout requires all cart items to use the same currency");
            }
            if (firstProduct.getAccessType() != product.getAccessType()) {
                throw new IllegalArgumentException("Stripe checkout does not support mixing subscriptions and one-time purchases");
            }
            if (product.getAccessType() == ProductAccessType.SUBSCRIPTION
                    && firstProduct.getBillingInterval() != product.getBillingInterval()) {
                throw new IllegalArgumentException("Stripe checkout requires subscription cart items to share the same billing interval");
            }
        }
    }

    private List<PaymentTransactionItem> buildTransactionItems(PaymentTransaction transaction, List<ResolvedCheckoutItem> resolvedItems) {
        List<PaymentTransactionItem> items = new ArrayList<>();
        for (ResolvedCheckoutItem resolvedItem : resolvedItems) {
            PaymentTransactionItem item = new PaymentTransactionItem();
            item.setTransaction(transaction);
            item.setProduct(resolvedItem.product());
            item.setQuantity(resolvedItem.quantity());
            item.setUnitPrice(resolvedItem.unitPrice());
            item.setLineAmount(resolvedItem.lineAmount());
            item.setCurrency(resolvedItem.product().getCurrency());
            items.add(item);
        }
        return items;
    }

    private record ResolvedCheckoutItem(DataProduct product, int quantity, BigDecimal unitPrice, BigDecimal lineAmount) {}
}
