package com.example.springbootjavarefresh.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MarketData {

    private Long id;

    @NotBlank
    private String symbol;

    @NotNull
    private BigDecimal price;

    private Long volume;

    @NotNull
    private LocalDateTime timestamp;

    private LocalDate marketDate;

    private MarketDataType dataType;

    public MarketData() {
    }

    public MarketData(String symbol, BigDecimal price, Long volume) {
        this(symbol, price, volume, MarketDataType.OTHER);
    }

    public MarketData(String symbol, BigDecimal price, Long volume, MarketDataType dataType) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = LocalDateTime.now();
        this.dataType = dataType;
        this.marketDate = this.timestamp.toLocalDate();
    }

    public void normalize() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (marketDate == null) {
            marketDate = timestamp.toLocalDate();
        }
        if (dataType == null) {
            dataType = MarketDataType.OTHER;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDate getMarketDate() {
        return marketDate;
    }

    public void setMarketDate(LocalDate marketDate) {
        this.marketDate = marketDate;
    }

    public MarketDataType getDataType() {
        return dataType;
    }

    public void setDataType(MarketDataType dataType) {
        this.dataType = dataType;
    }
}
