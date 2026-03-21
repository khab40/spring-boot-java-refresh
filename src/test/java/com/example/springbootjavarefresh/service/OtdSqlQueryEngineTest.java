package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OtdSqlQueryEngineTest {

    private final OtdSqlQueryEngine engine = new OtdSqlQueryEngine();

    @Test
    void shouldFilterAndSortRowsUsingSupportedSql() {
        MarketData aapl = marketData(1L, "AAPL", "189.42", 1_000L, "2026-03-20T10:15:00", MarketDataType.TICK);
        MarketData msft = marketData(2L, "MSFT", "414.15", 900L, "2026-03-20T10:14:00", MarketDataType.TICK);
        MarketData olderAapl = marketData(3L, "AAPL", "188.11", 800L, "2026-03-19T10:15:00", MarketDataType.TICK);

        List<MarketData> result = engine.execute(
                "SELECT * FROM market_data WHERE symbol = 'AAPL' AND market_date >= '2026-03-20' ORDER BY timestamp DESC LIMIT 5",
                List.of(msft, olderAapl, aapl)
        );

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void shouldRejectUnsupportedSelectList() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> engine.execute("SELECT symbol FROM market_data", List.of())
        );

        assertEquals("The OTD SQL engine currently supports SELECT * only.", exception.getMessage());
    }

    @Test
    void shouldSupportInClausesNumericFiltersAndAscendingOrder() {
        MarketData aapl = marketData(1L, "AAPL", "189.42", 1_000L, "2026-03-20T10:15:00", MarketDataType.TICK);
        MarketData msft = marketData(2L, "MSFT", "414.15", 900L, "2026-03-20T10:14:00", MarketDataType.TICK);
        MarketData nvda = marketData(3L, "NVDA", "920.11", 2_000L, "2026-03-20T10:16:00", MarketDataType.QUOTE);

        List<MarketData> result = engine.execute(
                "SELECT * FROM market_data WHERE symbol in ('MSFT', 'AAPL') AND volume >= 900 AND price < 500 ORDER BY symbol ASC",
                List.of(nvda, msft, aapl)
        );

        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("MSFT", result.get(1).getSymbol());
    }

    @Test
    void shouldRejectUnsupportedWhereCondition() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> engine.execute("SELECT * FROM market_data WHERE exchange = 'NASDAQ'", List.of())
        );

        assertEquals("Unsupported WHERE clause condition: exchange = 'NASDAQ'", exception.getMessage());
    }

    @Test
    void shouldRejectUnsupportedStringComparisonOperator() {
        MarketData aapl = marketData(1L, "AAPL", "189.42", 1_000L, "2026-03-20T10:15:00", MarketDataType.TICK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> engine.execute("SELECT * FROM market_data WHERE symbol > 'AAPL'", List.of(aapl))
        );

        assertEquals("Only equality is supported for string fields.", exception.getMessage());
    }

    private MarketData marketData(
            Long id,
            String symbol,
            String price,
            Long volume,
            String timestamp,
            MarketDataType marketDataType) {
        MarketData marketData = new MarketData(symbol, new BigDecimal(price), volume, marketDataType);
        marketData.setId(id);
        marketData.setTimestamp(LocalDateTime.parse(timestamp));
        marketData.setMarketDate(LocalDateTime.parse(timestamp).toLocalDate());
        return marketData;
    }
}
