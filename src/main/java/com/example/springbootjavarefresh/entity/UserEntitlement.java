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
@Table(name = "user_entitlements")
public class UserEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private DataProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private ProductAccessType accessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EntitlementStatus status = EntitlementStatus.ACTIVE;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "source_transaction_id")
    private Long sourceTransactionId;

    @Column(name = "batch_download_used_mb", nullable = false, precision = 12, scale = 2)
    private BigDecimal batchDownloadUsedMb = BigDecimal.ZERO;

    @Column(name = "realtime_subscriptions_used", nullable = false)
    private Integer realtimeSubscriptionsUsed = 0;

    @Column(name = "payload_kilobytes_used", nullable = false)
    private Long payloadKilobytesUsed = 0L;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
        if (batchDownloadUsedMb == null) {
            batchDownloadUsedMb = BigDecimal.ZERO;
        }
        if (realtimeSubscriptionsUsed == null) {
            realtimeSubscriptionsUsed = 0;
        }
        if (payloadKilobytesUsed == null) {
            payloadKilobytesUsed = 0L;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ProductAccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(ProductAccessType accessType) {
        this.accessType = accessType;
    }

    public EntitlementStatus getStatus() {
        return status;
    }

    public void setStatus(EntitlementStatus status) {
        this.status = status;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getSourceTransactionId() {
        return sourceTransactionId;
    }

    public void setSourceTransactionId(Long sourceTransactionId) {
        this.sourceTransactionId = sourceTransactionId;
    }

    public BigDecimal getBatchDownloadUsedMb() {
        return batchDownloadUsedMb;
    }

    public void setBatchDownloadUsedMb(BigDecimal batchDownloadUsedMb) {
        this.batchDownloadUsedMb = batchDownloadUsedMb;
    }

    public Integer getRealtimeSubscriptionsUsed() {
        return realtimeSubscriptionsUsed;
    }

    public void setRealtimeSubscriptionsUsed(Integer realtimeSubscriptionsUsed) {
        this.realtimeSubscriptionsUsed = realtimeSubscriptionsUsed;
    }

    public Long getPayloadKilobytesUsed() {
        return payloadKilobytesUsed;
    }

    public void setPayloadKilobytesUsed(Long payloadKilobytesUsed) {
        this.payloadKilobytesUsed = payloadKilobytesUsed;
    }
}
