package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;

import java.util.List;
import java.util.Optional;

public interface MarketDataStore {

    default boolean isStubMode() {
        return false;
    }

    default String getMode() {
        return "active";
    }

    default String getStatusMessage() {
        return "Market data is backed by the active runtime store.";
    }

    List<MarketData> findAll();

    Optional<MarketData> findById(Long id);

    List<MarketData> findBySymbol(String symbol);

    MarketData save(MarketData marketData);

    void deleteById(Long id);
}
