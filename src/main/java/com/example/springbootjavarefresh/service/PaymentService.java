package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.PaymentCheckoutRequest;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.PaymentTransactionRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final DataProductRepository dataProductRepository;
    private final PaymentAsyncProcessor paymentAsyncProcessor;

    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository,
            DataProductRepository dataProductRepository,
            PaymentAsyncProcessor paymentAsyncProcessor) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.dataProductRepository = dataProductRepository;
        this.paymentAsyncProcessor = paymentAsyncProcessor;
    }

    public PaymentTransaction initiateCheckout(PaymentCheckoutRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
        DataProduct product = dataProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Data product not found: " + request.getProductId()));

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(user);
        transaction.setProduct(product);
        transaction.setAmount(product.getPrice());
        transaction.setCurrency(product.getCurrency());
        transaction.setStatus(PaymentTransactionStatus.PENDING);

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        paymentAsyncProcessor.createCheckoutSessionAsync(saved.getId(), request.getSuccessUrl(), request.getCancelUrl());
        return saved;
    }

    public Optional<PaymentTransaction> getTransactionById(Long id) {
        return paymentTransactionRepository.findById(id);
    }
}
