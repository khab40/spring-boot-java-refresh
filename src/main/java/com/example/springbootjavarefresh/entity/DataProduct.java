package com.example.springbootjavarefresh.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_products")
public class DataProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    @JsonIgnore
    private DataCatalogItem catalogItem;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotBlank
    @Column(name = "currency", nullable = false)
    private String currency = "usd";

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private ProductAccessType accessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    private BillingInterval billingInterval = BillingInterval.ONE_TIME;

    @Column(name = "batch_download_limit_mb", precision = 12, scale = 2)
    private BigDecimal batchDownloadLimitMb;

    @Column(name = "minimum_price", precision = 12, scale = 2)
    private BigDecimal minimumPrice;

    @Column(name = "included_symbols")
    private Integer includedSymbols;

    @Column(name = "included_days")
    private Integer includedDays;

    @Column(name = "price_per_additional_symbol", precision = 12, scale = 2)
    private BigDecimal pricePerAdditionalSymbol;

    @Column(name = "price_per_additional_day", precision = 12, scale = 2)
    private BigDecimal pricePerAdditionalDay;

    @Column(name = "full_universe_symbol_count")
    private Integer fullUniverseSymbolCount;

    @Column(name = "realtime_subscription_limit")
    private Integer realtimeSubscriptionLimit;

    @Column(name = "max_realtime_payload_kb")
    private Integer maxRealtimePayloadKb;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    private BigDecimal quotedPrice;

    @Transient
    private Integer quotedSymbolCount;

    @Transient
    private Integer quotedDayCount;

    @Transient
    private LocalDate quotedStartDate;

    @Transient
    private LocalDate quotedEndDate;

    @Transient
    private String quotedSelectionSummary;

    @Transient
    private String quotedPricingSummary;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (currency != null) {
            currency = currency.toLowerCase();
        }
        normalizePricingDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        if (currency != null) {
            currency = currency.toLowerCase();
        }
        normalizePricingDefaults();
    }

    private void normalizePricingDefaults() {
        if (minimumPrice == null) {
            minimumPrice = price;
        }
        if (includedSymbols == null) {
            includedSymbols = 1;
        }
        if (includedDays == null) {
            includedDays = 1;
        }
        if (pricePerAdditionalSymbol == null) {
            pricePerAdditionalSymbol = BigDecimal.ZERO;
        }
        if (pricePerAdditionalDay == null) {
            pricePerAdditionalDay = BigDecimal.ZERO;
        }
        if (fullUniverseSymbolCount == null) {
            fullUniverseSymbolCount = includedSymbols;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataCatalogItem getCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(DataCatalogItem catalogItem) {
        this.catalogItem = catalogItem;
    }

    @JsonProperty("catalogItemId")
    public Long getCatalogItemId() {
        return catalogItem == null ? null : catalogItem.getId();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ProductAccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(ProductAccessType accessType) {
        this.accessType = accessType;
    }

    public BillingInterval getBillingInterval() {
        return billingInterval;
    }

    public void setBillingInterval(BillingInterval billingInterval) {
        this.billingInterval = billingInterval;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BigDecimal getBatchDownloadLimitMb() {
        return batchDownloadLimitMb;
    }

    public void setBatchDownloadLimitMb(BigDecimal batchDownloadLimitMb) {
        this.batchDownloadLimitMb = batchDownloadLimitMb;
    }

    public BigDecimal getMinimumPrice() {
        return minimumPrice;
    }

    public void setMinimumPrice(BigDecimal minimumPrice) {
        this.minimumPrice = minimumPrice;
    }

    public Integer getIncludedSymbols() {
        return includedSymbols;
    }

    public void setIncludedSymbols(Integer includedSymbols) {
        this.includedSymbols = includedSymbols;
    }

    public Integer getIncludedDays() {
        return includedDays;
    }

    public void setIncludedDays(Integer includedDays) {
        this.includedDays = includedDays;
    }

    public BigDecimal getPricePerAdditionalSymbol() {
        return pricePerAdditionalSymbol;
    }

    public void setPricePerAdditionalSymbol(BigDecimal pricePerAdditionalSymbol) {
        this.pricePerAdditionalSymbol = pricePerAdditionalSymbol;
    }

    public BigDecimal getPricePerAdditionalDay() {
        return pricePerAdditionalDay;
    }

    public void setPricePerAdditionalDay(BigDecimal pricePerAdditionalDay) {
        this.pricePerAdditionalDay = pricePerAdditionalDay;
    }

    public Integer getFullUniverseSymbolCount() {
        return fullUniverseSymbolCount;
    }

    public void setFullUniverseSymbolCount(Integer fullUniverseSymbolCount) {
        this.fullUniverseSymbolCount = fullUniverseSymbolCount;
    }

    public Integer getRealtimeSubscriptionLimit() {
        return realtimeSubscriptionLimit;
    }

    public void setRealtimeSubscriptionLimit(Integer realtimeSubscriptionLimit) {
        this.realtimeSubscriptionLimit = realtimeSubscriptionLimit;
    }

    public Integer getMaxRealtimePayloadKb() {
        return maxRealtimePayloadKb;
    }

    public void setMaxRealtimePayloadKb(Integer maxRealtimePayloadKb) {
        this.maxRealtimePayloadKb = maxRealtimePayloadKb;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getQuotedPrice() {
        return quotedPrice;
    }

    public void setQuotedPrice(BigDecimal quotedPrice) {
        this.quotedPrice = quotedPrice;
    }

    public Integer getQuotedSymbolCount() {
        return quotedSymbolCount;
    }

    public void setQuotedSymbolCount(Integer quotedSymbolCount) {
        this.quotedSymbolCount = quotedSymbolCount;
    }

    public Integer getQuotedDayCount() {
        return quotedDayCount;
    }

    public void setQuotedDayCount(Integer quotedDayCount) {
        this.quotedDayCount = quotedDayCount;
    }

    public LocalDate getQuotedStartDate() {
        return quotedStartDate;
    }

    public void setQuotedStartDate(LocalDate quotedStartDate) {
        this.quotedStartDate = quotedStartDate;
    }

    public LocalDate getQuotedEndDate() {
        return quotedEndDate;
    }

    public void setQuotedEndDate(LocalDate quotedEndDate) {
        this.quotedEndDate = quotedEndDate;
    }

    public String getQuotedSelectionSummary() {
        return quotedSelectionSummary;
    }

    public void setQuotedSelectionSummary(String quotedSelectionSummary) {
        this.quotedSelectionSummary = quotedSelectionSummary;
    }

    public String getQuotedPricingSummary() {
        return quotedPricingSummary;
    }

    public void setQuotedPricingSummary(String quotedPricingSummary) {
        this.quotedPricingSummary = quotedPricingSummary;
    }
}
