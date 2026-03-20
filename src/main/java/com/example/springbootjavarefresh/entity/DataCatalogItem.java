package com.example.springbootjavarefresh.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_catalog_items")
public class DataCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "summary")
    private String summary;

    @Column(name = "description", length = 4000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "market_data_type", nullable = false)
    private MarketDataType marketDataType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_system", nullable = false)
    private DataCatalogStorage storageSystem = DataCatalogStorage.STUB;

    @Column(name = "delivery_api_path")
    private String deliveryApiPath;

    @Column(name = "lake_query_reference")
    private String lakeQueryReference;

    @Column(name = "sample_symbols")
    private String sampleSymbols;

    @Column(name = "coverage_start_date")
    private LocalDate coverageStartDate;

    @Column(name = "coverage_end_date")
    private LocalDate coverageEndDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "catalogItem", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<DataProduct> offers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MarketDataType getMarketDataType() {
        return marketDataType;
    }

    public void setMarketDataType(MarketDataType marketDataType) {
        this.marketDataType = marketDataType;
    }

    public DataCatalogStorage getStorageSystem() {
        return storageSystem;
    }

    public void setStorageSystem(DataCatalogStorage storageSystem) {
        this.storageSystem = storageSystem;
    }

    public String getDeliveryApiPath() {
        return deliveryApiPath;
    }

    public void setDeliveryApiPath(String deliveryApiPath) {
        this.deliveryApiPath = deliveryApiPath;
    }

    public String getLakeQueryReference() {
        return lakeQueryReference;
    }

    public void setLakeQueryReference(String lakeQueryReference) {
        this.lakeQueryReference = lakeQueryReference;
    }

    public String getSampleSymbols() {
        return sampleSymbols;
    }

    public void setSampleSymbols(String sampleSymbols) {
        this.sampleSymbols = sampleSymbols;
    }

    public LocalDate getCoverageStartDate() {
        return coverageStartDate;
    }

    public void setCoverageStartDate(LocalDate coverageStartDate) {
        this.coverageStartDate = coverageStartDate;
    }

    public LocalDate getCoverageEndDate() {
        return coverageEndDate;
    }

    public void setCoverageEndDate(LocalDate coverageEndDate) {
        this.coverageEndDate = coverageEndDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DataProduct> getOffers() {
        return offers;
    }

    public void setOffers(List<DataProduct> offers) {
        this.offers = offers;
    }
}
