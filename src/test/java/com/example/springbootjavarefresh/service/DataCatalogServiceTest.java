package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.ProductAccessType;
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
    private DataProductRepository dataProductRepository;

    @InjectMocks
    private DataCatalogService dataCatalogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldForceOneTimeBillingForOneTimeProducts() {
        CreateDataProductRequest request = new CreateDataProductRequest();
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
        when(dataProductRepository.save(org.mockito.ArgumentMatchers.any(DataProduct.class))).thenReturn(savedProduct);

        DataProduct result = dataCatalogService.createProduct(request);

        ArgumentCaptor<DataProduct> captor = ArgumentCaptor.forClass(DataProduct.class);
        verify(dataProductRepository).save(captor.capture());
        DataProduct persisted = captor.getValue();
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
}
