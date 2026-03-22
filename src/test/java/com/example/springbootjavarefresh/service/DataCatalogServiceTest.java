package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CreateCatalogItemRequest;
import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataCatalogStorage;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.MarketDataType;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.repository.DataCatalogItemRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataCatalogServiceTest {

    @Mock
    private DataCatalogItemRepository dataCatalogItemRepository;

    @Mock
    private DataProductRepository dataProductRepository;

    @Mock
    private CatalogPricingService catalogPricingService;

    @InjectMocks
    private DataCatalogService dataCatalogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldForceOneTimeBillingForOneTimeProducts() {
        DataCatalogItem item = new DataCatalogItem();
        item.setId(7L);

        CreateDataProductRequest request = new CreateDataProductRequest();
        request.setCatalogItemId(7L);
        request.setCode("BOOK-ONCE");
        request.setName("Reference Book");
        request.setDescription("One-off purchase");
        request.setPrice(new BigDecimal("149.00"));
        request.setCurrency("USD");
        request.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        request.setBillingInterval(BillingInterval.YEARLY);
        request.setBatchDownloadLimitMb(new BigDecimal("250.00"));
        request.setMinimumPrice(new BigDecimal("149.00"));
        request.setIncludedSymbols(2);
        request.setIncludedDays(5);
        request.setPricePerAdditionalSymbol(new BigDecimal("3.50"));
        request.setPricePerAdditionalDay(new BigDecimal("1.25"));
        request.setFullUniverseSymbolCount(500);
        request.setRealtimeSubscriptionLimit(3);
        request.setMaxRealtimePayloadKb(512);

        DataProduct savedProduct = new DataProduct();
        savedProduct.setId(11L);
        when(dataCatalogItemRepository.findById(7L)).thenReturn(java.util.Optional.of(item));
        when(dataProductRepository.save(org.mockito.ArgumentMatchers.any(DataProduct.class))).thenReturn(savedProduct);

        DataProduct result = dataCatalogService.createProduct(request);

        ArgumentCaptor<DataProduct> captor = ArgumentCaptor.forClass(DataProduct.class);
        verify(dataProductRepository).save(captor.capture());
        DataProduct persisted = captor.getValue();
        assertEquals(7L, persisted.getCatalogItemId());
        assertEquals("BOOK-ONCE", persisted.getCode());
        assertEquals(ProductAccessType.ONE_TIME_PURCHASE, persisted.getAccessType());
        assertEquals(BillingInterval.ONE_TIME, persisted.getBillingInterval());
        assertEquals(new BigDecimal("149.00"), persisted.getPrice());
        assertEquals(new BigDecimal("149.00"), persisted.getMinimumPrice());
        assertEquals(2, persisted.getIncludedSymbols());
        assertEquals(5, persisted.getIncludedDays());
        assertEquals(new BigDecimal("3.50"), persisted.getPricePerAdditionalSymbol());
        assertEquals(new BigDecimal("1.25"), persisted.getPricePerAdditionalDay());
        assertEquals(500, persisted.getFullUniverseSymbolCount());
        assertEquals("USD", persisted.getCurrency());
        assertEquals(new BigDecimal("250.00"), persisted.getBatchDownloadLimitMb());
        assertEquals(3, persisted.getRealtimeSubscriptionLimit());
        assertEquals(512, persisted.getMaxRealtimePayloadKb());
        assertEquals(11L, result.getId());
    }

    @Test
    void shouldCreateCatalogItemMetadata() {
        CreateCatalogItemRequest request = new CreateCatalogItemRequest();
        request.setCode("US-EQ-QUOTE");
        request.setName("US Equities Quotes");
        request.setSummary("Best bid and ask snapshots");
        request.setDescription("Catalog metadata for the core quote lake dataset.");
        request.setMarketDataType(MarketDataType.QUOTE);
        request.setStorageSystem(DataCatalogStorage.DELTA_LAKE);
        request.setDeliveryApiPath("/api/market-data/query");
        request.setLakeQueryReference("lake.us_equities_quotes");
        request.setSampleSymbols("AAPL,NVDA,GOOGL");

        DataCatalogItem savedItem = new DataCatalogItem();
        savedItem.setId(20L);
        when(dataCatalogItemRepository.save(org.mockito.ArgumentMatchers.any(DataCatalogItem.class))).thenReturn(savedItem);

        DataCatalogItem result = dataCatalogService.createCatalogItem(request);

        ArgumentCaptor<DataCatalogItem> captor = ArgumentCaptor.forClass(DataCatalogItem.class);
        verify(dataCatalogItemRepository).save(captor.capture());
        DataCatalogItem persisted = captor.getValue();
        assertEquals("US-EQ-QUOTE", persisted.getCode());
        assertEquals(DataCatalogStorage.DELTA_LAKE, persisted.getStorageSystem());
        assertEquals(MarketDataType.QUOTE, persisted.getMarketDataType());
        assertEquals("lake.us_equities_quotes", persisted.getLakeQueryReference());
        assertEquals(20L, result.getId());
    }

    @Test
    void shouldFilterCatalogItemsBySymbolCoverageAndOfferMode() {
        DataCatalogItem aapl = new DataCatalogItem();
        aapl.setId(1L);
        aapl.setCode("AAPL-QUOTE");
        aapl.setName("AAPL Quotes");
        aapl.setSummary("Apple quote history");
        aapl.setDescription("Historical Apple quotes");
        aapl.setMarketDataType(MarketDataType.QUOTE);
        aapl.setStorageSystem(DataCatalogStorage.DELTA_LAKE);
        aapl.setSampleSymbols("AAPL,MSFT");
        aapl.setCoverageStartDate(LocalDate.of(2026, 3, 1));
        aapl.setCoverageEndDate(LocalDate.of(2026, 3, 31));
        aapl.setActive(true);

        DataCatalogItem meta = new DataCatalogItem();
        meta.setId(2L);
        meta.setCode("META-QUOTE");
        meta.setName("META Quotes");
        meta.setSummary("Meta quote history");
        meta.setDescription("Historical Meta quotes");
        meta.setMarketDataType(MarketDataType.QUOTE);
        meta.setStorageSystem(DataCatalogStorage.DELTA_LAKE);
        meta.setSampleSymbols("META");
        meta.setCoverageStartDate(LocalDate.of(2026, 1, 1));
        meta.setCoverageEndDate(LocalDate.of(2026, 1, 31));
        meta.setActive(true);

        DataProduct matchingOffer = new DataProduct();
        matchingOffer.setId(11L);
        matchingOffer.setCode("AAPL-STREAM");
        matchingOffer.setName("AAPL Stream");
        matchingOffer.setAccessType(ProductAccessType.SUBSCRIPTION);
        matchingOffer.setBillingInterval(BillingInterval.MONTHLY);
        matchingOffer.setPrice(new BigDecimal("25.00"));
        matchingOffer.setActive(true);

        DataProduct nonMatchingOffer = new DataProduct();
        nonMatchingOffer.setId(12L);
        nonMatchingOffer.setCode("META-DOWNLOAD");
        nonMatchingOffer.setName("META Download");
        nonMatchingOffer.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        nonMatchingOffer.setBillingInterval(BillingInterval.ONE_TIME);
        nonMatchingOffer.setPrice(new BigDecimal("10.00"));
        nonMatchingOffer.setActive(true);

        when(dataCatalogItemRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(aapl, meta));
        when(dataProductRepository.findByCatalogItem_IdOrderByPriceAsc(1L)).thenReturn(List.of(matchingOffer));
        when(dataProductRepository.findByCatalogItem_IdOrderByPriceAsc(2L)).thenReturn(List.of(nonMatchingOffer));
        when(catalogPricingService.quote(org.mockito.ArgumentMatchers.eq(aapl), org.mockito.ArgumentMatchers.eq(matchingOffer), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CatalogPricingService.CatalogPriceQuote(
                        new BigDecimal("39.00"),
                        1,
                        16,
                        LocalDate.of(2026, 3, 5),
                        LocalDate.of(2026, 3, 20),
                        "AAPL | 2026-03-05 to 2026-03-20",
                        "base 25.00 + 0 extra symbol(s) + 15 extra day(s)"
                ));
        when(catalogPricingService.quote(org.mockito.ArgumentMatchers.eq(meta), org.mockito.ArgumentMatchers.eq(nonMatchingOffer), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CatalogPricingService.CatalogPriceQuote(
                        new BigDecimal("10.00"),
                        1,
                        1,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 1),
                        "META | 2026-01-01 to 2026-01-01",
                        "base 10.00 + 0 extra symbol(s) + 0 extra day(s)"
                ));

        List<com.example.springbootjavarefresh.dto.CatalogItemResponse> results = dataCatalogService.searchCatalogItems(
                true,
                "AAPL",
                LocalDateTime.of(2026, 3, 5, 9, 30),
                LocalDateTime.of(2026, 3, 20, 16, 0),
                MarketDataType.QUOTE,
                DataCatalogStorage.DELTA_LAKE,
                ProductAccessType.SUBSCRIPTION,
                BillingInterval.MONTHLY
        );

        assertEquals(1, results.size());
        assertEquals("AAPL-QUOTE", results.getFirst().code());
        assertEquals(1, results.getFirst().offers().size());
        assertEquals("AAPL-STREAM", results.getFirst().offers().getFirst().getCode());
        assertEquals(new BigDecimal("39.00"), results.getFirst().offers().getFirst().getQuotedPrice());
        assertEquals("AAPL | 2026-03-05 to 2026-03-20", results.getFirst().selectionSummary());
    }
}
