package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.MarketDataRuntimeStatusResponse;
import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
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
    private MarketDataStore marketDataStore;

    @InjectMocks
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllMarketData() {
        MarketData data1 = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L, MarketDataType.TICK);
        MarketData data2 = new MarketData("GOOGL", BigDecimal.valueOf(2500.00), 500L, MarketDataType.FUNDAMENTALS);
        when(marketDataStore.findAll()).thenReturn(Arrays.asList(data1, data2));

        List<MarketData> result = marketDataService.getAllMarketData();

        assertEquals(2, result.size());
        verify(marketDataStore, times(1)).findAll();
    }

    @Test
    void testGetMarketDataById() {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L, MarketDataType.TICK);
        data.setId(1L);
        when(marketDataStore.findById(1L)).thenReturn(Optional.of(data));

        Optional<MarketData> result = marketDataService.getMarketDataById(1L);

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getSymbol());
        verify(marketDataStore, times(1)).findById(1L);
    }

    @Test
    void testSaveMarketData() {
        MarketData data = new MarketData("AAPL", BigDecimal.valueOf(150.00), 1000L, MarketDataType.TICK);
        when(marketDataStore.save(any(MarketData.class))).thenReturn(data);

        MarketData result = marketDataService.saveMarketData(data);

        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals(MarketDataType.TICK, result.getDataType());
        assertNotNull(result.getMarketDate());
        verify(marketDataStore, times(1)).save(data);
    }

    @Test
    void testDeleteMarketData() {
        doNothing().when(marketDataStore).deleteById(1L);

        marketDataService.deleteMarketData(1L);

        verify(marketDataStore, times(1)).deleteById(1L);
    }

    @Test
    void testGetRuntimeStatus() {
        when(marketDataStore.getMode()).thenReturn("stub");
        when(marketDataStore.isStubMode()).thenReturn(true);
        when(marketDataStore.getStatusMessage()).thenReturn("Stub mode enabled.");

        MarketDataRuntimeStatusResponse result = marketDataService.getRuntimeStatus();

        assertEquals("stub", result.mode());
        assertTrue(result.stubbed());
        assertEquals("Stub mode enabled.", result.message());
    }
}
