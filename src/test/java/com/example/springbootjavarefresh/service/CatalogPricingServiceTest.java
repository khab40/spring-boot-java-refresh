package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CatalogSelectionRequest;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogPricingServiceTest {

    private final CatalogPricingService catalogPricingService = new CatalogPricingService();

    @Test
    void shouldCalculateDynamicPriceFromSelectedSymbolsAndDates() {
        DataCatalogItem item = new DataCatalogItem();
        item.setCoverageStartDate(LocalDate.of(2026, 3, 1));
        item.setCoverageEndDate(LocalDate.of(2026, 3, 31));
        item.setSampleSymbols("AAPL,MSFT,NVDA");

        DataProduct product = new DataProduct();
        product.setPrice(new BigDecimal("25.00"));
        product.setMinimumPrice(new BigDecimal("25.00"));
        product.setIncludedSymbols(1);
        product.setIncludedDays(1);
        product.setPricePerAdditionalSymbol(new BigDecimal("3.00"));
        product.setPricePerAdditionalDay(new BigDecimal("0.50"));
        product.setFullUniverseSymbolCount(500);

        CatalogSelectionRequest selection = new CatalogSelectionRequest();
        selection.setSymbol("AAPL,MSFT,NVDA");
        selection.setAvailableFrom(LocalDateTime.of(2026, 3, 5, 9, 30));
        selection.setAvailableTo(LocalDateTime.of(2026, 3, 7, 16, 0));

        CatalogPricingService.CatalogPriceQuote quote = catalogPricingService.quote(item, product, selection);

        assertEquals(new BigDecimal("32.00"), quote.quotedPrice());
        assertEquals(3, quote.quotedSymbolCount());
        assertEquals(3, quote.quotedDayCount());
        assertEquals("AAPL,MSFT,NVDA | 2026-03-05 to 2026-03-07", quote.quotedSelectionSummary());
    }

    @Test
    void shouldFallbackToFullUniverseWhenWildcardSelectionIsUsed() {
        DataCatalogItem item = new DataCatalogItem();
        item.setCoverageStartDate(LocalDate.of(2026, 1, 1));
        item.setCoverageEndDate(LocalDate.of(2026, 1, 31));
        item.setSampleSymbols("BTCUSD,ETHUSD");

        DataProduct product = new DataProduct();
        product.setPrice(new BigDecimal("10.00"));
        product.setMinimumPrice(new BigDecimal("10.00"));
        product.setIncludedSymbols(2);
        product.setIncludedDays(10);
        product.setPricePerAdditionalSymbol(new BigDecimal("1.00"));
        product.setPricePerAdditionalDay(new BigDecimal("0.25"));
        product.setFullUniverseSymbolCount(100);

        CatalogSelectionRequest selection = new CatalogSelectionRequest();
        selection.setSymbol("*");

        CatalogPricingService.CatalogPriceQuote quote = catalogPricingService.quote(item, product, selection);

        assertEquals(100, quote.quotedSymbolCount());
        assertEquals(31, quote.quotedDayCount());
        assertEquals(new BigDecimal("113.25"), quote.quotedPrice());
    }
}
