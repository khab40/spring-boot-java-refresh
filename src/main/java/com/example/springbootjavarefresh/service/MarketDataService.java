package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.repository.MarketDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MarketDataService {

    @Autowired
    private MarketDataRepository marketDataRepository;

    public List<MarketData> getAllMarketData() {
        return marketDataRepository.findAll();
    }

    public Optional<MarketData> getMarketDataById(Long id) {
        return marketDataRepository.findById(id);
    }

    public List<MarketData> getMarketDataBySymbol(String symbol) {
        return marketDataRepository.findBySymbol(symbol);
    }

    public MarketData saveMarketData(MarketData marketData) {
        return marketDataRepository.save(marketData);
    }

    public void deleteMarketData(Long id) {
        marketDataRepository.deleteById(id);
    }
}