package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MarketDataService {

    private final MarketDataStore marketDataStore;

    public MarketDataService(MarketDataStore marketDataStore) {
        this.marketDataStore = marketDataStore;
    }

    public List<MarketData> getAllMarketData() {
        return marketDataStore.findAll();
    }

    public Optional<MarketData> getMarketDataById(Long id) {
        return marketDataStore.findById(id);
    }

    public List<MarketData> getMarketDataBySymbol(String symbol) {
        return marketDataStore.findBySymbol(symbol);
    }

    public MarketData saveMarketData(MarketData marketData) {
        marketData.normalize();
        return marketDataStore.save(marketData);
    }

    public void deleteMarketData(Long id) {
        marketDataStore.deleteById(id);
    }
}
