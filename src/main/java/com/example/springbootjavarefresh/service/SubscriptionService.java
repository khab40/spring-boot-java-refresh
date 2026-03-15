package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.Subscription;
import com.example.springbootjavarefresh.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public List<Subscription> getSubscriptionsByUserId(String userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public List<Subscription> getActiveSubscriptionsByUserId(String userId) {
        return subscriptionRepository.findByUserIdAndActive(userId, true);
    }

    public Optional<Subscription> getSubscriptionByUserIdAndSymbol(String userId, String symbol) {
        return subscriptionRepository.findByUserIdAndSymbol(userId, symbol);
    }

    public Subscription saveSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public void deleteSubscription(Long id) {
        subscriptionRepository.deleteById(id);
    }

    public boolean subscribe(String userId, String symbol) {
        Optional<Subscription> existing = subscriptionRepository.findByUserIdAndSymbol(userId, symbol);
        if (existing.isPresent()) {
            Subscription sub = existing.get();
            if (!sub.getActive()) {
                sub.setActive(true);
                subscriptionRepository.save(sub);
                return true;
            }
            return false; // already subscribed
        }
        Subscription newSub = new Subscription(userId, symbol);
        subscriptionRepository.save(newSub);
        return true;
    }

    public boolean unsubscribe(String userId, String symbol) {
        Optional<Subscription> existing = subscriptionRepository.findByUserIdAndSymbol(userId, symbol);
        if (existing.isPresent() && existing.get().getActive()) {
            Subscription sub = existing.get();
            sub.setActive(false);
            subscriptionRepository.save(sub);
            return true;
        }
        return false; // not subscribed or already inactive
    }
}