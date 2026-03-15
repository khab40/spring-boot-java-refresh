package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    List<MarketData> findBySymbol(String symbol);

    List<MarketData> findBySymbolOrderByTimestampDesc(String symbol);
}