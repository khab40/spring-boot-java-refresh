package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateDataProductRequest {

    @NotNull
    private Long catalogItemId;

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private BigDecimal price;

    @NotBlank
    private String currency;

    @NotNull
    private ProductAccessType accessType;

    private BillingInterval billingInterval = BillingInterval.ONE_TIME;

    private BigDecimal batchDownloadLimitMb;

    private BigDecimal minimumPrice;

    private Integer includedSymbols;

    private Integer includedDays;

    private BigDecimal pricePerAdditionalSymbol;

    private BigDecimal pricePerAdditionalDay;

    private Integer fullUniverseSymbolCount;

    private Integer realtimeSubscriptionLimit;

    private Integer maxRealtimePayloadKb;

    public String getCode() {
        return code;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
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

    public BigDecimal getBatchDownloadLimitMb() {
        return batchDownloadLimitMb;
    }

    public void setBatchDownloadLimitMb(BigDecimal batchDownloadLimitMb) {
        this.batchDownloadLimitMb = batchDownloadLimitMb;
    }

    public Integer getRealtimeSubscriptionLimit() {
        return realtimeSubscriptionLimit;
    }

    public void setRealtimeSubscriptionLimit(Integer realtimeSubscriptionLimit) {
        this.realtimeSubscriptionLimit = realtimeSubscriptionLimit;
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

    public Integer getMaxRealtimePayloadKb() {
        return maxRealtimePayloadKb;
    }

    public void setMaxRealtimePayloadKb(Integer maxRealtimePayloadKb) {
        this.maxRealtimePayloadKb = maxRealtimePayloadKb;
    }
}
