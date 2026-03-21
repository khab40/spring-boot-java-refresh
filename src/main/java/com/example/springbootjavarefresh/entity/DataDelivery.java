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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_deliveries")
public class DataDelivery {

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
    @Column(name = "status", nullable = false)
    private DataDeliveryStatus status = DataDeliveryStatus.READY;

    @Lob
    @Column(name = "sql_text", nullable = false)
    private String sqlText;

    @Lob
    @Column(name = "object_keys", nullable = false)
    private String objectKeys;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount = 0;

    @Column(name = "file_count", nullable = false)
    private Integer fileCount = 0;

    @Column(name = "total_bytes", nullable = false)
    private Long totalBytes = 0L;

    @Column(name = "consumed_megabytes", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumedMegabytes = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (rowCount == null) {
            rowCount = 0;
        }
        if (fileCount == null) {
            fileCount = 0;
        }
        if (totalBytes == null) {
            totalBytes = 0L;
        }
        if (consumedMegabytes == null) {
            consumedMegabytes = BigDecimal.ZERO;
        }
        if (status == null) {
            status = DataDeliveryStatus.READY;
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

    public DataDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DataDeliveryStatus status) {
        this.status = status;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getObjectKeys() {
        return objectKeys;
    }

    public void setObjectKeys(String objectKeys) {
        this.objectKeys = objectKeys;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public BigDecimal getConsumedMegabytes() {
        return consumedMegabytes;
    }

    public void setConsumedMegabytes(BigDecimal consumedMegabytes) {
        this.consumedMegabytes = consumedMegabytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
