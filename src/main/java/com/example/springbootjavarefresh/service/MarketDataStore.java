package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;

import java.util.List;
import java.util.Optional;

public interface MarketDataStore {

    List<MarketData> findAll();

    Optional<MarketData> findById(Long id);

    List<MarketData> findBySymbol(String symbol);

    MarketData save(MarketData marketData);

    void deleteById(Long id);
}
