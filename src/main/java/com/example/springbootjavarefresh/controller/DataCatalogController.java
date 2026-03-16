package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.service.DataCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Data Catalog", description = "API for listing and managing available data products")
public class DataCatalogController {

    private final DataCatalogService dataCatalogService;

    public DataCatalogController(DataCatalogService dataCatalogService) {
        this.dataCatalogService = dataCatalogService;
    }

    @GetMapping("/products")
    @Operation(summary = "Get all catalog products")
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
    @Operation(summary = "Create a catalog product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataProduct> createProduct(@Valid @RequestBody CreateDataProductRequest request) {
        return ResponseEntity.ok(dataCatalogService.createProduct(request));
    }
}
