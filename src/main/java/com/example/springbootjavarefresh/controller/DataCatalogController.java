package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.CatalogItemResponse;
import com.example.springbootjavarefresh.dto.CreateCatalogItemRequest;
import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataCatalogStorage;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.MarketDataType;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.service.DataCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Data Catalog", description = "API for listing available lake datasets and linked sellable offers")
public class DataCatalogController {

    private final DataCatalogService dataCatalogService;

    public DataCatalogController(DataCatalogService dataCatalogService) {
        this.dataCatalogService = dataCatalogService;
    }

    @GetMapping("/items")
    @Operation(summary = "Get all catalog items with linked sellable offers")
    public ResponseEntity<List<CatalogItemResponse>> getAllCatalogItems(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableTo,
            @RequestParam(required = false) MarketDataType marketDataType,
            @RequestParam(required = false) DataCatalogStorage storageSystem,
            @RequestParam(required = false) ProductAccessType accessType,
            @RequestParam(required = false) BillingInterval billingInterval) {
        boolean hasFilters = isPresent(symbol)
                || availableFrom != null
                || availableTo != null
                || marketDataType != null
                || storageSystem != null
                || accessType != null
                || billingInterval != null;

        if (!hasFilters) {
            return ResponseEntity.ok(activeOnly ? dataCatalogService.getActiveCatalogItems() : dataCatalogService.getAllCatalogItems());
        }

        return ResponseEntity.ok(dataCatalogService.searchCatalogItems(
                activeOnly,
                symbol,
                availableFrom,
                availableTo,
                marketDataType,
                storageSystem,
                accessType,
                billingInterval
        ));
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Get catalog item by ID")
    public ResponseEntity<CatalogItemResponse> getCatalogItemById(@PathVariable Long id) {
        return dataCatalogService.getCatalogItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/items/code/{code}")
    @Operation(summary = "Get catalog item by code")
    public ResponseEntity<CatalogItemResponse> getCatalogItemByCode(@PathVariable String code) {
        return dataCatalogService.getCatalogItemByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    @Operation(summary = "Create a catalog item")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataCatalogItem> createCatalogItem(@Valid @RequestBody CreateCatalogItemRequest request) {
        return ResponseEntity.ok(dataCatalogService.createCatalogItem(request));
    }

    @GetMapping("/products")
    @Operation(summary = "Get all sellable catalog offers")
    public ResponseEntity<List<DataProduct>> getAllProducts(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? dataCatalogService.getActiveProducts() : dataCatalogService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get catalog product by ID")
    public ResponseEntity<DataProduct> getProductById(@PathVariable Long id) {
        return dataCatalogService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/code/{code}")
    @Operation(summary = "Get catalog product by code")
    public ResponseEntity<DataProduct> getProductByCode(@PathVariable String code) {
        return dataCatalogService.getProductByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    @Operation(summary = "Create a sellable catalog offer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataProduct> createProduct(@Valid @RequestBody CreateDataProductRequest request) {
        return ResponseEntity.ok(dataCatalogService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update a sellable catalog offer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataProduct> updateProduct(@PathVariable Long id, @Valid @RequestBody CreateDataProductRequest request) {
        return ResponseEntity.ok(dataCatalogService.updateProduct(id, request));
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
