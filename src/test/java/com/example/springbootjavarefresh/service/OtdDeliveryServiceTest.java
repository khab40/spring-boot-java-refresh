package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.OtdDeliveryFileResponse;
import com.example.springbootjavarefresh.dto.OtdDeliveryRequest;
import com.example.springbootjavarefresh.dto.OtdDeliveryResponse;
import com.example.springbootjavarefresh.entity.DataDelivery;
import com.example.springbootjavarefresh.entity.DataDeliveryStatus;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.DataDeliveryRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtdDeliveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DataProductRepository dataProductRepository;

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @Mock
    private DataDeliveryRepository dataDeliveryRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private OtdSqlQueryEngine otdSqlQueryEngine;

    @Mock
    private ParquetExportService parquetExportService;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private DataDeliveryEmailService dataDeliveryEmailService;

    private OtdDeliveryService otdDeliveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        otdDeliveryService = new OtdDeliveryService(
                userRepository,
                dataProductRepository,
                userEntitlementRepository,
                dataDeliveryRepository,
                marketDataService,
                otdSqlQueryEngine,
                parquetExportService,
                objectStorageService,
                dataDeliveryEmailService,
                1000
        );
    }

    @Test
    void shouldCreateDeliveryAndDeductBatchAllowance() {
        User user = user(7L, "delivery@example.com");
        DataProduct product = oneTimeProduct(11L, "AAPL-OTD", new BigDecimal("50.00"));
        UserEntitlement entitlement = activeEntitlement(user, product, new BigDecimal("10.00"), 1);
        MarketData row = marketData(100L, "AAPL", "189.42", 1_000L, "2026-03-20T12:00:00", MarketDataType.TICK);

        byte[] parquetPayload = new byte[2_048];
        ParquetExportService.ExportedParquetPart export = new ParquetExportService.ExportedParquetPart("aapl.parquet", parquetPayload);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(dataProductRepository.findById(11L)).thenReturn(Optional.of(product));
        when(userEntitlementRepository.findByUserIdAndProductId(7L, 11L)).thenReturn(Optional.of(entitlement));
        when(marketDataService.getAllMarketData()).thenReturn(List.of(row));
        when(otdSqlQueryEngine.execute(any(), any())).thenReturn(List.of(row));
        when(parquetExportService.export(any(), any(), any(Integer.class))).thenReturn(List.of(export));
        when(objectStorageService.signGetUrl(any())).thenReturn(
                new ObjectStorageService.SignedObjectUrl("http://localhost:9000/signed/aapl.parquet", LocalDateTime.parse("2026-03-21T12:00:00"))
        );
        when(dataDeliveryRepository.save(any(DataDelivery.class))).thenAnswer(invocation -> {
            DataDelivery delivery = invocation.getArgument(0);
            delivery.setId(22L);
            return delivery;
        });

        OtdDeliveryResponse response = otdDeliveryService.createDelivery(
                7L,
                new OtdDeliveryRequest(11L, "SELECT * FROM market_data WHERE symbol = 'AAPL'")
        );

        assertEquals(22L, response.deliveryId());
        assertEquals("AAPL-OTD", response.productCode());
        assertEquals(1, response.rowCount());
        assertEquals(1, response.files().size());
        assertNotNull(response.remainingBatchMegabytes());
        assertEquals(new BigDecimal("10.01"), entitlement.getBatchDownloadUsedMb());

        verify(objectStorageService).upload(any(), any(), any());
        verify(userEntitlementRepository).save(entitlement);
        verify(dataDeliveryEmailService).sendDeliveryEmail(user, response);

        ArgumentCaptor<DataDelivery> deliveryCaptor = ArgumentCaptor.forClass(DataDelivery.class);
        verify(dataDeliveryRepository).save(deliveryCaptor.capture());
        assertEquals("SELECT * FROM market_data WHERE symbol = 'AAPL'", deliveryCaptor.getValue().getSqlText());
    }

    @Test
    void shouldRejectNonOneTimeProduct() {
        User user = user(7L, "delivery@example.com");
        DataProduct product = oneTimeProduct(11L, "AAPL-OTD", new BigDecimal("50.00"));
        product.setAccessType(ProductAccessType.SUBSCRIPTION);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(dataProductRepository.findById(11L)).thenReturn(Optional.of(product));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> otdDeliveryService.createDelivery(7L, new OtdDeliveryRequest(11L, "SELECT * FROM market_data"))
        );

        assertEquals("OTD delivery is supported only for one-time purchase offers.", exception.getMessage());
        verify(userEntitlementRepository, never()).findByUserIdAndProductId(any(), any());
    }

    @Test
    void shouldRejectExpiredEntitlement() {
        User user = user(7L, "delivery@example.com");
        DataProduct product = oneTimeProduct(11L, "AAPL-OTD", new BigDecimal("50.00"));
        UserEntitlement entitlement = activeEntitlement(user, product, BigDecimal.ZERO, 1);
        entitlement.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(dataProductRepository.findById(11L)).thenReturn(Optional.of(product));
        when(userEntitlementRepository.findByUserIdAndProductId(7L, 11L)).thenReturn(Optional.of(entitlement));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> otdDeliveryService.createDelivery(7L, new OtdDeliveryRequest(11L, "SELECT * FROM market_data"))
        );

        assertEquals("The selected entitlement has expired.", exception.getMessage());
        verify(parquetExportService, never()).export(any(), any(), any(Integer.class));
    }

    @Test
    void shouldRejectWhenBatchDownloadLimitWouldBeExceeded() {
        User user = user(7L, "delivery@example.com");
        DataProduct product = oneTimeProduct(11L, "AAPL-OTD", new BigDecimal("50.00"));
        UserEntitlement entitlement = activeEntitlement(user, product, new BigDecimal("50.00"), 1);
        MarketData row = marketData(100L, "AAPL", "189.42", 1_000L, "2026-03-20T12:00:00", MarketDataType.TICK);

        byte[] parquetPayload = new byte[5_242_880];
        ParquetExportService.ExportedParquetPart export = new ParquetExportService.ExportedParquetPart("aapl.parquet", parquetPayload);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(dataProductRepository.findById(11L)).thenReturn(Optional.of(product));
        when(userEntitlementRepository.findByUserIdAndProductId(7L, 11L)).thenReturn(Optional.of(entitlement));
        when(marketDataService.getAllMarketData()).thenReturn(List.of(row));
        when(otdSqlQueryEngine.execute(any(), any())).thenReturn(List.of(row));
        when(parquetExportService.export(any(), any(), any(Integer.class))).thenReturn(List.of(export));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> otdDeliveryService.createDelivery(7L, new OtdDeliveryRequest(11L, "SELECT * FROM market_data"))
        );

        assertEquals("Batch download limit exceeded for product AAPL-OTD", exception.getMessage());
        verify(objectStorageService, never()).upload(any(), any(), any());
        verify(dataDeliveryRepository, never()).save(any());
    }

    @Test
    void shouldReturnDeliveryHistoryWithSignedFilesAndRemainingAllowance() {
        User user = user(7L, "delivery@example.com");
        DataProduct product = oneTimeProduct(11L, "AAPL-OTD", new BigDecimal("25.00"));
        UserEntitlement entitlement = activeEntitlement(user, product, new BigDecimal("2.50"), 2);
        DataDelivery delivery = new DataDelivery();
        delivery.setId(91L);
        delivery.setUser(user);
        delivery.setProduct(product);
        delivery.setStatus(DataDeliveryStatus.READY);
        delivery.setSqlText("SELECT * FROM market_data WHERE symbol in ('AAPL','MSFT')");
        delivery.setObjectKeys("""
                otd/7/2026/03/20/120000/aapl.parquet\t128
                otd/7/2026/03/20/120000/msft.parquet\t256
                """.trim());
        delivery.setRowCount(2);
        delivery.setFileCount(2);
        delivery.setTotalBytes(384L);
        delivery.setConsumedMegabytes(new BigDecimal("0.02"));
        delivery.setCreatedAt(LocalDateTime.parse("2026-03-20T12:00:00"));

        when(dataDeliveryRepository.findAllByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(delivery));
        when(userEntitlementRepository.findByUserIdAndProductId(7L, 11L)).thenReturn(Optional.of(entitlement));
        when(objectStorageService.signGetUrl("otd/7/2026/03/20/120000/aapl.parquet")).thenReturn(
                new ObjectStorageService.SignedObjectUrl("http://localhost:9000/signed/aapl.parquet", LocalDateTime.parse("2026-03-21T12:00:00"))
        );
        when(objectStorageService.signGetUrl("otd/7/2026/03/20/120000/msft.parquet")).thenReturn(
                new ObjectStorageService.SignedObjectUrl("http://localhost:9000/signed/msft.parquet", LocalDateTime.parse("2026-03-21T12:05:00"))
        );

        List<OtdDeliveryResponse> deliveries = otdDeliveryService.getDeliveriesForUser(7L);

        assertEquals(1, deliveries.size());
        OtdDeliveryResponse response = deliveries.get(0);
        assertEquals(new BigDecimal("47.50"), response.remainingBatchMegabytes());
        assertEquals(2, response.files().size());

        OtdDeliveryFileResponse firstFile = response.files().get(0);
        assertEquals("aapl.parquet", firstFile.fileName());
        assertEquals(128L, firstFile.sizeBytes());
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Delivery");
        user.setLastName("User");
        user.setPasswordHash("ignored");
        return user;
    }

    private DataProduct oneTimeProduct(Long id, String code, BigDecimal batchLimitMb) {
        DataProduct product = new DataProduct();
        product.setId(id);
        product.setCode(code);
        product.setName(code + " name");
        product.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        product.setBatchDownloadLimitMb(batchLimitMb);
        product.setPrice(new BigDecimal("99.00"));
        product.setCurrency("usd");
        return product;
    }

    private UserEntitlement activeEntitlement(User user, DataProduct product, BigDecimal usedMb, int purchasedUnits) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUser(user);
        entitlement.setProduct(product);
        entitlement.setAccessType(ProductAccessType.ONE_TIME_PURCHASE);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setPurchasedUnits(purchasedUnits);
        entitlement.setBatchDownloadUsedMb(usedMb);
        return entitlement;
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
