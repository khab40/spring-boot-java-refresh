package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.DataCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class DataCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataCatalogService dataCatalogService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

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
}
