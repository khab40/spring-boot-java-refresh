package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.dto.CatalogItemResponse;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataCatalogStorage;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.MarketDataType;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.DataCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DataCatalogControllerTest {
private MockMvc mockMvc;
    @Mock
    private DataCatalogService dataCatalogService;
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private UserDetailsService userDetailsService;
    @InjectMocks
    private DataCatalogController dataCatalogController;



    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dataCatalogController).build();
    }

@Test
    void shouldReturnCatalogItems() throws Exception {
        CatalogItemResponse item = new CatalogItemResponse(
                5L,
                "US-EQ-QUOTE",
                "US Equities Quotes",
                "Best bid and ask snapshots",
                "Detailed catalog item metadata",
                MarketDataType.QUOTE.name(),
                DataCatalogStorage.DELTA_LAKE.name(),
                "/api/market-data/query",
                "lake.us_equities_quotes",
                "AAPL,NVDA",
                null,
                null,
                "AAPL,NVDA | 2026-03-01 to 2026-03-01",
                true,
                null,
                List.of()
        );
        when(dataCatalogService.getAllCatalogItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/catalog/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("US-EQ-QUOTE"))
                .andExpect(jsonPath("$[0].storageSystem").value("DELTA_LAKE"));
    }

    @Test
    void shouldReturnFilteredCatalogItems() throws Exception {
        CatalogItemResponse item = new CatalogItemResponse(
                7L,
                "AAPL-QUOTE",
                "AAPL Quotes",
                "Apple quote history",
                "Apple quote history in the lake",
                MarketDataType.QUOTE.name(),
                DataCatalogStorage.DELTA_LAKE.name(),
                "/api/market-data/query",
                "lake.aapl_quotes",
                "AAPL",
                null,
                null,
                "AAPL | 2026-03-01 to 2026-03-31",
                true,
                null,
                List.of()
        );
        when(dataCatalogService.searchCatalogItems(
                eq(true),
                eq("AAPL"),
                any(),
                any(),
                eq(MarketDataType.QUOTE),
                eq(DataCatalogStorage.DELTA_LAKE),
                eq(ProductAccessType.SUBSCRIPTION),
                eq(BillingInterval.MONTHLY)
        )).thenReturn(List.of(item));

        mockMvc.perform(get("/api/catalog/items")
                        .param("activeOnly", "true")
                        .param("symbol", "AAPL")
                        .param("availableFrom", "2026-03-01T09:30:00")
                        .param("availableTo", "2026-03-31T16:00:00")
                        .param("marketDataType", "QUOTE")
                        .param("storageSystem", "DELTA_LAKE")
                        .param("accessType", "SUBSCRIPTION")
                        .param("billingInterval", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("AAPL-QUOTE"));
    }

    @Test
    void shouldReturnCatalogProducts() throws Exception {
        DataProduct product = new DataProduct();
        product.setId(10L);
        product.setCode("L2-EQUITIES");
        product.setName("Level 2 Equities");
        product.setPrice(new BigDecimal("49.99"));
        product.setCurrency("usd");
        product.setAccessType(ProductAccessType.SUBSCRIPTION);
        product.setBillingInterval(BillingInterval.MONTHLY);
        when(dataCatalogService.getAllProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("L2-EQUITIES"));
    }

    @Test
    void shouldReturnOnlyActiveProducts() throws Exception {
        DataProduct product = new DataProduct();
        product.setId(10L);
        product.setCode("ACTIVE-ONLY");
        when(dataCatalogService.getActiveProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/catalog/products").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACTIVE-ONLY"));
    }

    @Test
    void shouldGetProductByCode() throws Exception {
        DataProduct product = new DataProduct();
        product.setId(12L);
        product.setCode("FX-TICKS");
        product.setName("FX Tick Feed");
        when(dataCatalogService.getProductByCode("FX-TICKS")).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/catalog/products/code/FX-TICKS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FX Tick Feed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateProduct() throws Exception {
        DataProduct product = new DataProduct();
        product.setId(15L);
        product.setCode("OPTIONS-PRO");
        product.setAccessType(ProductAccessType.SUBSCRIPTION);
        product.setBillingInterval(BillingInterval.MONTHLY);
        when(dataCatalogService.createProduct(any())).thenReturn(product);

        mockMvc.perform(post("/api/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogItemId": 5,
                                  "code": "OPTIONS-PRO",
                                  "name": "Options Pro",
                                  "description": "Full options chain package",
                                  "price": 79.99,
                                  "currency": "usd",
                                  "accessType": "SUBSCRIPTION",
                                  "billingInterval": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.code").value("OPTIONS-PRO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateProduct() throws Exception {
        DataProduct product = new DataProduct();
        product.setId(15L);
        product.setCode("OPTIONS-PRO");
        product.setPrice(new BigDecimal("99.99"));
        when(dataCatalogService.updateProduct(eq(15L), any())).thenReturn(product);

        mockMvc.perform(put("/api/catalog/products/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogItemId": 5,
                                  "code": "OPTIONS-PRO",
                                  "name": "Options Pro",
                                  "description": "Full options chain package",
                                  "price": 99.99,
                                  "minimumPrice": 79.99,
                                  "includedSymbols": 5,
                                  "includedDays": 30,
                                  "pricePerAdditionalSymbol": 2.50,
                                  "pricePerAdditionalDay": 0.75,
                                  "fullUniverseSymbolCount": 800,
                                  "currency": "usd",
                                  "accessType": "SUBSCRIPTION",
                                  "billingInterval": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateCatalogItem() throws Exception {
        DataCatalogItem item = new DataCatalogItem();
        item.setId(5L);
        item.setCode("US-EQ-QUOTE");
        when(dataCatalogService.createCatalogItem(any())).thenReturn(item);

        mockMvc.perform(post("/api/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "US-EQ-QUOTE",
                                  "name": "US Equities Quotes",
                                  "summary": "Best bid and ask snapshots",
                                  "description": "Detailed catalog item metadata",
                                  "marketDataType": "QUOTE",
                                  "storageSystem": "DELTA_LAKE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.code").value("US-EQ-QUOTE"));
    }
}