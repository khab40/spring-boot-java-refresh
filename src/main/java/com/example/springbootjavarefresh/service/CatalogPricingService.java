package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CatalogSelectionRequest;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class CatalogPricingService {

    public CatalogPriceQuote quote(DataCatalogItem item, DataProduct product, CatalogSelectionRequest selection) {
        CatalogSelection normalizedSelection = normalizeSelection(item, product, selection);

        BigDecimal additionalSymbolCharge = safe(product.getPricePerAdditionalSymbol())
                .multiply(BigDecimal.valueOf(Math.max(0, normalizedSelection.symbolCount() - defaultInt(product.getIncludedSymbols(), 1))));
        BigDecimal additionalDayCharge = safe(product.getPricePerAdditionalDay())
                .multiply(BigDecimal.valueOf(Math.max(0, normalizedSelection.dayCount() - defaultInt(product.getIncludedDays(), 1))));

        BigDecimal quotedPrice = safe(product.getPrice())
                .add(additionalSymbolCharge)
                .add(additionalDayCharge)
                .max(safe(product.getMinimumPrice() == null ? product.getPrice() : product.getMinimumPrice()))
                .setScale(2, RoundingMode.HALF_UP);

        return new CatalogPriceQuote(
                quotedPrice,
                normalizedSelection.symbolCount(),
                normalizedSelection.dayCount(),
                normalizedSelection.startDate(),
                normalizedSelection.endDate(),
                normalizedSelection.selectionSummary(),
                "base "
                        + quotedPriceString(product.getPrice())
                        + " + "
                        + normalizedSelection.additionalSymbols()
                        + " extra symbol(s) + "
                        + normalizedSelection.additionalDays()
                        + " extra day(s)"
        );
    }

    public CatalogSelectionRequest fromFilters(String symbol, LocalDateTime availableFrom, LocalDateTime availableTo) {
        CatalogSelectionRequest selection = new CatalogSelectionRequest();
        selection.setSymbol(symbol);
        selection.setAvailableFrom(availableFrom);
        selection.setAvailableTo(availableTo);
        return selection;
    }

    private CatalogSelection normalizeSelection(DataCatalogItem item, DataProduct product, CatalogSelectionRequest selection) {
        LocalDate coverageStart = item.getCoverageStartDate();
        LocalDate coverageEnd = item.getCoverageEndDate();

        LocalDate requestedStart = selection == null || selection.getAvailableFrom() == null ? null : selection.getAvailableFrom().toLocalDate();
        LocalDate requestedEnd = selection == null || selection.getAvailableTo() == null ? null : selection.getAvailableTo().toLocalDate();

        LocalDate effectiveStart = requestedStart != null ? requestedStart : coverageStart;
        LocalDate effectiveEnd = requestedEnd != null ? requestedEnd : coverageEnd;
        if (effectiveStart == null && effectiveEnd == null) {
            effectiveStart = LocalDate.now();
            effectiveEnd = effectiveStart;
        } else if (effectiveStart == null) {
            effectiveStart = effectiveEnd;
        } else if (effectiveEnd == null) {
            effectiveEnd = effectiveStart;
        }
        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate swap = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = swap;
        }

        int symbolCount = resolveSymbolCount(item, product, selection == null ? null : selection.getSymbol());
        int dayCount = (int) ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
        int additionalSymbols = Math.max(0, symbolCount - defaultInt(product.getIncludedSymbols(), 1));
        int additionalDays = Math.max(0, dayCount - defaultInt(product.getIncludedDays(), 1));

        String requestedSymbol = selection == null ? null : selection.getSymbol();
        String symbolLabel = requestedSymbol == null || requestedSymbol.isBlank()
                ? defaultSelectionLabel(item, product)
                : requestedSymbol.trim();

        return new CatalogSelection(
                symbolCount,
                dayCount,
                additionalSymbols,
                additionalDays,
                effectiveStart,
                effectiveEnd,
                symbolLabel + " | " + effectiveStart + " to " + effectiveEnd
        );
    }

    private int resolveSymbolCount(DataCatalogItem item, DataProduct product, String symbolExpression) {
        if (symbolExpression == null || symbolExpression.isBlank() || "*".equals(symbolExpression.trim())) {
            if (product.getFullUniverseSymbolCount() != null && product.getFullUniverseSymbolCount() > 0) {
                return product.getFullUniverseSymbolCount();
            }
            Set<String> sampleSymbols = splitSymbols(item.getSampleSymbols());
            return sampleSymbols.isEmpty() ? 1 : sampleSymbols.size();
        }

        return splitSymbols(symbolExpression).size();
    }

    private Set<String> splitSymbols(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }

        Set<String> symbols = new LinkedHashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter((symbol) -> !symbol.isBlank())
                .forEach(symbols::add);
        return symbols;
    }

    private String defaultSelectionLabel(DataCatalogItem item, DataProduct product) {
        if (product.getFullUniverseSymbolCount() != null && product.getFullUniverseSymbolCount() > 1) {
            return "FULL UNIVERSE";
        }
        Set<String> sampleSymbols = splitSymbols(item.getSampleSymbols());
        if (sampleSymbols.isEmpty()) {
            return "DEFAULT SELECTION";
        }
        return String.join(",", sampleSymbols);
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null || value < 1 ? fallback : value;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String quotedPriceString(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public record CatalogPriceQuote(
            BigDecimal quotedPrice,
            int quotedSymbolCount,
            int quotedDayCount,
            LocalDate quotedStartDate,
            LocalDate quotedEndDate,
            String quotedSelectionSummary,
            String quotedPricingSummary
    ) {
    }

    private record CatalogSelection(
            int symbolCount,
            int dayCount,
            int additionalSymbols,
            int additionalDays,
            LocalDate startDate,
            LocalDate endDate,
            String selectionSummary
    ) {
    }
}
