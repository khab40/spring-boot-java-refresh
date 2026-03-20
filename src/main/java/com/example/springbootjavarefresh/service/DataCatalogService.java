package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CatalogItemResponse;
import com.example.springbootjavarefresh.dto.CreateCatalogItemRequest;
import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.repository.DataCatalogItemRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DataCatalogService {

    private final DataCatalogItemRepository dataCatalogItemRepository;
    private final DataProductRepository dataProductRepository;

    public DataCatalogService(
            DataCatalogItemRepository dataCatalogItemRepository,
            DataProductRepository dataProductRepository) {
        this.dataCatalogItemRepository = dataCatalogItemRepository;
        this.dataProductRepository = dataProductRepository;
    }

    public List<CatalogItemResponse> getAllCatalogItems() {
        return dataCatalogItemRepository.findAllByOrderByNameAsc().stream()
                .map(this::toCatalogItemResponse)
                .toList();
    }

    public List<CatalogItemResponse> getActiveCatalogItems() {
        return dataCatalogItemRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toCatalogItemResponse)
                .toList();
    }

    public Optional<CatalogItemResponse> getCatalogItemById(Long id) {
        return dataCatalogItemRepository.findById(id).map(this::toCatalogItemResponse);
    }

    public Optional<CatalogItemResponse> getCatalogItemByCode(String code) {
        return dataCatalogItemRepository.findByCode(code).map(this::toCatalogItemResponse);
    }

    public DataCatalogItem createCatalogItem(CreateCatalogItemRequest request) {
        DataCatalogItem item = new DataCatalogItem();
        item.setCode(request.getCode());
        item.setName(request.getName());
        item.setSummary(request.getSummary());
        item.setDescription(request.getDescription());
        item.setMarketDataType(request.getMarketDataType());
        item.setStorageSystem(request.getStorageSystem());
        item.setDeliveryApiPath(request.getDeliveryApiPath());
        item.setLakeQueryReference(request.getLakeQueryReference());
        item.setSampleSymbols(request.getSampleSymbols());
        item.setCoverageStartDate(request.getCoverageStartDate());
        item.setCoverageEndDate(request.getCoverageEndDate());
        item.setActive(request.getActive());
        return dataCatalogItemRepository.save(item);
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
        DataCatalogItem catalogItem = dataCatalogItemRepository.findById(request.getCatalogItemId())
                .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + request.getCatalogItemId()));

        DataProduct product = new DataProduct();
        product.setCatalogItem(catalogItem);
        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setAccessType(request.getAccessType());
        product.setBillingInterval(request.getBillingInterval());
        product.setBatchDownloadLimitMb(request.getBatchDownloadLimitMb());
        product.setRealtimeSubscriptionLimit(request.getRealtimeSubscriptionLimit());
        product.setMaxRealtimePayloadKb(request.getMaxRealtimePayloadKb());
        return dataProductRepository.save(product);
    }

    private CatalogItemResponse toCatalogItemResponse(DataCatalogItem item) {
        return CatalogItemResponse.from(item, dataProductRepository.findByCatalogItem_IdOrderByPriceAsc(item.getId()));
    }
}
