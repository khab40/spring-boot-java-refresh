package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.DataCatalogStorage;
import com.example.springbootjavarefresh.entity.MarketDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateCatalogItemRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String summary;

    private String description;

    @NotNull
    private MarketDataType marketDataType;

    @NotNull
    private DataCatalogStorage storageSystem;

    private String deliveryApiPath;

    private String lakeQueryReference;

    private String sampleSymbols;

    private LocalDate coverageStartDate;

    private LocalDate coverageEndDate;

    private Boolean active = true;

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
}
