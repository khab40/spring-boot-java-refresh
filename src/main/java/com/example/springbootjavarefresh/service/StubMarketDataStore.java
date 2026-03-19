package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Primary
public class StubMarketDataStore implements MarketDataStore {

    private final boolean stubEnabled;
    private final ConcurrentMap<Long, MarketData> entries = new ConcurrentHashMap<>();
    private final AtomicLong identifiers = new AtomicLong(10_000);

    public StubMarketDataStore(@Value("${marketdata.stub.enabled:true}") boolean stubEnabled) {
        this.stubEnabled = stubEnabled;
        seed(
                marketData("AAPL", "189.42", 12_500L, LocalDateTime.now().minusMinutes(15), MarketDataType.TICK),
                marketData("MSFT", "414.15", 8_200L, LocalDateTime.now().minusMinutes(30), MarketDataType.TICK),
                marketData("NVDA", "1020.88", 6_100L, LocalDateTime.now().minusHours(1), MarketDataType.TICK),
                marketData("CRYPTO-BASKET", "102.75", 850L, LocalDateTime.now().minusHours(2), MarketDataType.CRYPTO),
                marketData("MACRO-NEWS", "0.00", null, LocalDateTime.now().minusHours(3), MarketDataType.NEWS)
        );
    }

    @Override
    public boolean isStubMode() {
        return stubEnabled;
    }

    @Override
    public String getMode() {
        return "stub";
    }

    @Override
    public String getStatusMessage() {
        return stubEnabled
                ? "Delta Lake is isolated from runtime. Market data responses are served from an in-memory preview stub."
                : "Market data stubs are disabled.";
    }

    @Override
    public List<MarketData> findAll() {
        return entries.values().stream()
                .sorted(Comparator.comparing(MarketData::getTimestamp).reversed())
                .map(this::cloneEntry)
                .toList();
    }

    @Override
    public Optional<MarketData> findById(Long id) {
        return Optional.ofNullable(entries.get(id)).map(this::cloneEntry);
    }

    @Override
    public List<MarketData> findBySymbol(String symbol) {
        return entries.values().stream()
                .filter(entry -> entry.getSymbol().equalsIgnoreCase(symbol))
                .sorted(Comparator.comparing(MarketData::getTimestamp).reversed())
                .map(this::cloneEntry)
                .toList();
    }

    @Override
    public MarketData save(MarketData marketData) {
        MarketData copy = cloneEntry(marketData);
        if (copy.getId() == null) {
            copy.setId(identifiers.incrementAndGet());
        }
        if (copy.getTimestamp() == null) {
            copy.setTimestamp(LocalDateTime.now());
        }
        if (copy.getMarketDate() == null) {
            copy.setMarketDate(copy.getTimestamp().toLocalDate());
        }
        entries.put(copy.getId(), copy);
        return cloneEntry(copy);
    }

    @Override
    public void deleteById(Long id) {
        entries.remove(id);
    }

    private void seed(MarketData... marketDataRows) {
        for (MarketData row : marketDataRows) {
            save(row);
        }
    }

    private MarketData marketData(
            String symbol,
            String price,
            Long volume,
            LocalDateTime timestamp,
            MarketDataType dataType) {
        MarketData marketData = new MarketData(symbol, new BigDecimal(price), volume, dataType);
        marketData.setTimestamp(timestamp);
        marketData.setMarketDate(LocalDate.from(timestamp));
        return marketData;
    }

    private MarketData cloneEntry(MarketData source) {
        MarketData copy = new MarketData();
        copy.setId(source.getId());
        copy.setSymbol(source.getSymbol());
        copy.setPrice(source.getPrice());
        copy.setVolume(source.getVolume());
        copy.setTimestamp(source.getTimestamp());
        copy.setMarketDate(source.getMarketDate());
        copy.setDataType(source.getDataType());
        return copy;
    }
}
