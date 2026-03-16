package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DataCatalogService {

    private final DataProductRepository dataProductRepository;

    public DataCatalogService(DataProductRepository dataProductRepository) {
        this.dataProductRepository = dataProductRepository;
    }

    public List<DataProduct> getAllProducts() {
        return dataProductRepository.findAll();
    }

    public List<DataProduct> getActiveProducts() {
        return dataProductRepository.findByActiveTrue();
    }

    public Optional<DataProduct> getProductById(Long id) {
        return dataProductRepository.findById(id);
    }

    public Optional<DataProduct> getProductByCode(String code) {
        return dataProductRepository.findByCode(code);
    }

    public DataProduct createProduct(CreateDataProductRequest request) {
        if (request.getAccessType() == ProductAccessType.ONE_TIME_PURCHASE) {
            request.setBillingInterval(BillingInterval.ONE_TIME);
        }

        DataProduct product = new DataProduct();
        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setAccessType(request.getAccessType());
        product.setBillingInterval(request.getBillingInterval());
        return dataProductRepository.save(product);
    }
}
