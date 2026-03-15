package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.Subscription;
import com.example.springbootjavarefresh.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions", description = "API for managing market data subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Get all subscriptions")
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get subscriptions by user ID")
    public ResponseEntity<List<Subscription>> getSubscriptionsByUserId(@PathVariable String userId) {
        List<Subscription> subscriptions = subscriptionService.getSubscriptionsByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active subscriptions by user ID")
    public ResponseEntity<List<Subscription>> getActiveSubscriptionsByUserId(@PathVariable String userId) {
        List<Subscription> subscriptions = subscriptionService.getActiveSubscriptionsByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to market data for a symbol")
    public ResponseEntity<String> subscribe(@RequestParam String userId, @RequestParam String symbol) {
        boolean success = subscriptionService.subscribe(userId, symbol);
        if (success) {
            return ResponseEntity.ok("Subscribed successfully");
        } else {
            return ResponseEntity.badRequest().body("Already subscribed");
        }
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from market data for a symbol")
    public ResponseEntity<String> unsubscribe(@RequestParam String userId, @RequestParam String symbol) {
        boolean success = subscriptionService.unsubscribe(userId, symbol);
        if (success) {
            return ResponseEntity.ok("Unsubscribed successfully");
        } else {
            return ResponseEntity.badRequest().body("Not subscribed or already inactive");
        }
    }

    @PostMapping
    @Operation(summary = "Create new subscription")
    public ResponseEntity<Subscription> createSubscription(@Valid @RequestBody Subscription subscription) {
        Subscription saved = subscriptionService.saveSubscription(subscription);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subscription by ID")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}