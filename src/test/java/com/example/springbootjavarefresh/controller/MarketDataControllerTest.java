package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {
private MockMvc mockMvc;
    @Mock
    private MarketDataService marketDataService;
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @InjectMocks
    private MarketDataController marketDataController;



    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(marketDataController).build();
    }

@Test
    void testGetAllMarketData() throws Exception {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L, MarketDataType.TICK);
        when(marketDataService.getAllMarketData()).thenReturn(Arrays.asList(data));

        mockMvc.perform(get("/api/market-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].dataType").value("TICK"));

        verify(marketDataService, times(1)).getAllMarketData();
    }

    @Test
    void testGetMarketDataById() throws Exception {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L, MarketDataType.TICK);
        data.setId(1L);
        when(marketDataService.getMarketDataById(1L)).thenReturn(Optional.of(data));

        mockMvc.perform(get("/api/market-data/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));

        verify(marketDataService, times(1)).getMarketDataById(1L);
    }

    @Test
    void testGetMarketDataRuntime() throws Exception {
        when(marketDataService.getRuntimeStatus())
                .thenReturn(new com.example.springbootjavarefresh.dto.MarketDataRuntimeStatusResponse(
                        "stub",
                        true,
                        "Delta Lake is isolated from runtime."
                ));

        mockMvc.perform(get("/api/market-data/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("stub"))
                .andExpect(jsonPath("$.stubbed").value(true));

        verify(marketDataService, times(1)).getRuntimeStatus();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMarketData() throws Exception {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L, MarketDataType.CRYPTO);
        when(marketDataService.saveMarketData(any(MarketData.class))).thenReturn(data);

        mockMvc.perform(post("/api/market-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.dataType").value("CRYPTO"));

        verify(marketDataService, times(1)).saveMarketData(any(MarketData.class));
    }
}
