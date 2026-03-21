package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParquetExportServiceTest {

    private final ParquetExportService parquetExportService = new ParquetExportService();

    @Test
    void shouldRejectEmptyResultSets() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parquetExportService.export(List.of(), "empty", 1000)
        );

        assertEquals("The OTD query returned no rows.", exception.getMessage());
    }

    @Test
    void shouldSplitLargeExportsIntoMultipleParquetFiles() {
        List<MarketData> rows = List.of(
                marketData(1L, "AAPL", "189.42", 1_000L, "2026-03-20T10:00:00"),
                marketData(2L, "MSFT", "414.15", 900L, "2026-03-20T10:01:00"),
                marketData(3L, "NVDA", "920.12", 800L, "2026-03-20T10:02:00")
        );

        List<ParquetExportService.ExportedParquetPart> exports = parquetExportService.export(rows, "market-slice", 2);

        assertEquals(2, exports.size());
        assertEquals("market-slice-part-00001.parquet", exports.get(0).fileName());
        assertEquals("market-slice-part-00002.parquet", exports.get(1).fileName());
        assertTrue(exports.get(0).payload().length > 0);
        assertTrue(exports.get(1).payload().length > 0);
    }

    private MarketData marketData(Long id, String symbol, String price, Long volume, String timestamp) {
        MarketData marketData = new MarketData(symbol, new BigDecimal(price), volume, MarketDataType.TICK);
        marketData.setId(id);
        marketData.setTimestamp(LocalDateTime.parse(timestamp));
        marketData.setMarketDate(LocalDateTime.parse(timestamp).toLocalDate());
        return marketData;
    }
}
