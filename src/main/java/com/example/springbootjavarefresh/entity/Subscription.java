package com.example.springbootjavarefresh.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank
    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }

    // Constructors
    public Subscription() {}

    public Subscription(String userId, String symbol) {
        this.userId = userId;
        this.symbol = symbol;
        this.subscribedAt = LocalDateTime.now();
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}