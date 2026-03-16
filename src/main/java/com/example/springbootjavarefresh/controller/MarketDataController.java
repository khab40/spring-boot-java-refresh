package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
@Tag(name = "Market Data", description = "API for managing market data")
public class MarketDataController {

    @Autowired
    private MarketDataService marketDataService;

    @GetMapping
    @Operation(summary = "Get all market data")
    public ResponseEntity<List<MarketData>> getAllMarketData() {
        List<MarketData> marketData = marketDataService.getAllMarketData();
        return ResponseEntity.ok(marketData);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get market data by ID")
    public ResponseEntity<MarketData> getMarketDataById(@PathVariable Long id) {
        return marketDataService.getMarketDataById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    @Operation(summary = "Get market data by symbol")
    public ResponseEntity<List<MarketData>> getMarketDataBySymbol(@PathVariable String symbol) {
        List<MarketData> marketData = marketDataService.getMarketDataBySymbol(symbol);
        return ResponseEntity.ok(marketData);
    }

    @PostMapping
    @Operation(summary = "Create new market data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketData> createMarketData(@Valid @RequestBody MarketData marketData) {
        MarketData saved = marketDataService.saveMarketData(marketData);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete market data by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMarketData(@PathVariable Long id) {
        marketDataService.deleteMarketData(id);
        return ResponseEntity.noContent().build();
    }
}
