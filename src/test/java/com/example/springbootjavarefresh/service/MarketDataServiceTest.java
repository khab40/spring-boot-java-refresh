package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @InjectMocks
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllMarketData() {
        MarketData data1 = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L);
        MarketData data2 = new MarketData("GOOGL", BigDecimal.valueOf(2500.00), 500L);
        when(marketDataRepository.findAll()).thenReturn(Arrays.asList(data1, data2));

        List<MarketData> result = marketDataService.getAllMarketData();

        assertEquals(2, result.size());
        verify(marketDataRepository, times(1)).findAll();
    }

    @Test
    void testGetMarketDataById() {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L);
        data.setId(1L);
        when(marketDataRepository.findById(1L)).thenReturn(Optional.of(data));

        Optional<MarketData> result = marketDataService.getMarketDataById(1L);

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getSymbol());
        verify(marketDataRepository, times(1)).findById(1L);
    }

    @Test
    void testSaveMarketData() {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L);
        when(marketDataRepository.save(any(MarketData.class))).thenReturn(data);

        MarketData result = marketDataService.saveMarketData(data);

        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        verify(marketDataRepository, times(1)).save(data);
    }

    @Test
    void testDeleteMarketData() {
        doNothing().when(marketDataRepository).deleteById(1L);

        marketDataService.deleteMarketData(1L);

        verify(marketDataRepository, times(1)).deleteById(1L);
    }
}