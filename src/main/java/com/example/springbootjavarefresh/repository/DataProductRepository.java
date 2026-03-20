package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.DataProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataProductRepository extends JpaRepository<DataProduct, Long> {
    List<DataProduct> findByActiveTrue();
    Optional<DataProduct> findByCode(String code);
    List<DataProduct> findByCatalogItem_IdOrderByPriceAsc(Long catalogItemId);
}
