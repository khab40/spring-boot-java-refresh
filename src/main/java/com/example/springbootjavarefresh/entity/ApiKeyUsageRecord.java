package com.example.springbootjavarefresh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_key_usage_records")
public class ApiKeyUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private DataProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private UsageType usageType;

    @Column(name = "megabytes_used", nullable = false, precision = 12, scale = 2)
    private BigDecimal megabytesUsed = BigDecimal.ZERO;

    @Column(name = "payload_kilobytes_used", nullable = false)
    private Long payloadKilobytesUsed = 0L;

    @Column(name = "realtime_subscriptions_used", nullable = false)
    private Integer realtimeSubscriptionsUsed = 0;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 1;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
        if (megabytesUsed == null) {
            megabytesUsed = BigDecimal.ZERO;
        }
        if (payloadKilobytesUsed == null) {
            payloadKilobytesUsed = 0L;
        }
        if (realtimeSubscriptionsUsed == null) {
            realtimeSubscriptionsUsed = 0;
        }
        if (requestCount == null) {
            requestCount = 1;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DataProduct getProduct() {
        return product;
    }

    public void setProduct(DataProduct product) {
        this.product = product;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }

    public BigDecimal getMegabytesUsed() {
        return megabytesUsed;
    }

    public void setMegabytesUsed(BigDecimal megabytesUsed) {
        this.megabytesUsed = megabytesUsed;
    }

    public Long getPayloadKilobytesUsed() {
        return payloadKilobytesUsed;
    }

    public void setPayloadKilobytesUsed(Long payloadKilobytesUsed) {
        this.payloadKilobytesUsed = payloadKilobytesUsed;
    }

    public Integer getRealtimeSubscriptionsUsed() {
        return realtimeSubscriptionsUsed;
    }

    public void setRealtimeSubscriptionsUsed(Integer realtimeSubscriptionsUsed) {
        this.realtimeSubscriptionsUsed = realtimeSubscriptionsUsed;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
