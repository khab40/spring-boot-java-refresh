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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataCatalogServiceTest {

    @Mock
    private DataCatalogItemRepository dataCatalogItemRepository;

    @Mock
    private DataProductRepository dataProductRepository;

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
}
