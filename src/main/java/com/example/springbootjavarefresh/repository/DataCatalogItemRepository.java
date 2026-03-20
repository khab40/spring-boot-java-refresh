package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.DataCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataCatalogItemRepository extends JpaRepository<DataCatalogItem, Long> {
    List<DataCatalogItem> findByActiveTrueOrderByNameAsc();
    List<DataCatalogItem> findAllByOrderByNameAsc();
    Optional<DataCatalogItem> findByCode(String code);
}
