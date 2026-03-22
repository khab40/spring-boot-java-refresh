package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CatalogItemResponse;
import com.example.springbootjavarefresh.dto.CatalogSelectionRequest;
import com.example.springbootjavarefresh.dto.CreateCatalogItemRequest;
import com.example.springbootjavarefresh.dto.CreateDataProductRequest;
import com.example.springbootjavarefresh.entity.BillingInterval;
import com.example.springbootjavarefresh.entity.DataCatalogItem;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.DataCatalogStorage;
import com.example.springbootjavarefresh.entity.MarketDataType;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.repository.DataCatalogItemRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DataCatalogService {

    private final DataCatalogItemRepository dataCatalogItemRepository;
    private final DataProductRepository dataProductRepository;
    private final CatalogPricingService catalogPricingService;

    public DataCatalogService(
            DataCatalogItemRepository dataCatalogItemRepository,
            DataProductRepository dataProductRepository,
            CatalogPricingService catalogPricingService) {
        this.dataCatalogItemRepository = dataCatalogItemRepository;
        this.dataProductRepository = dataProductRepository;
        this.catalogPricingService = catalogPricingService;
    }

    public List<CatalogItemResponse> getAllCatalogItems() {
        return dataCatalogItemRepository.findAllByOrderByNameAsc().stream()
                .map((item) -> toCatalogItemResponse(item, null))
                .toList();
    }

    public List<CatalogItemResponse> getActiveCatalogItems() {
        return dataCatalogItemRepository.findByActiveTrueOrderByNameAsc().stream()
                .map((item) -> toCatalogItemResponse(item, null))
                .toList();
    }

    public List<CatalogItemResponse> searchCatalogItems(
            boolean activeOnly,
            String symbol,
            LocalDateTime availableFrom,
            LocalDateTime availableTo,
            MarketDataType marketDataType,
            DataCatalogStorage storageSystem,
            ProductAccessType accessType,
            BillingInterval billingInterval) {
        List<DataCatalogItem> items = activeOnly
                ? dataCatalogItemRepository.findByActiveTrueOrderByNameAsc()
                : dataCatalogItemRepository.findAllByOrderByNameAsc();

        CatalogSelectionRequest selection = catalogPricingService.fromFilters(symbol, availableFrom, availableTo);

        return items.stream()
                .map((item) -> toFilteredCatalogItemResponse(item, activeOnly, accessType, billingInterval, selection))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter((item) -> matchesSymbol(item, symbol))
                .filter((item) -> matchesCoverage(item, availableFrom, availableTo))
                .filter((item) -> marketDataType == null || marketDataType.name().equals(item.marketDataType()))
                .filter((item) -> storageSystem == null || storageSystem.name().equals(item.storageSystem()))
                .toList();
    }

    public Optional<CatalogItemResponse> getCatalogItemById(Long id) {
        return dataCatalogItemRepository.findById(id).map((item) -> toCatalogItemResponse(item, null));
    }

    public Optional<CatalogItemResponse> getCatalogItemByCode(String code) {
        return dataCatalogItemRepository.findByCode(code).map((item) -> toCatalogItemResponse(item, null));
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
        product.setMinimumPrice(request.getMinimumPrice());
        product.setIncludedSymbols(request.getIncludedSymbols());
        product.setIncludedDays(request.getIncludedDays());
        product.setPricePerAdditionalSymbol(request.getPricePerAdditionalSymbol());
        product.setPricePerAdditionalDay(request.getPricePerAdditionalDay());
        product.setFullUniverseSymbolCount(request.getFullUniverseSymbolCount());
        product.setRealtimeSubscriptionLimit(request.getRealtimeSubscriptionLimit());
        product.setMaxRealtimePayloadKb(request.getMaxRealtimePayloadKb());
        return dataProductRepository.save(product);
    }

    public DataProduct updateProduct(Long id, CreateDataProductRequest request) {
        DataProduct product = dataProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data product not found: " + id));
        DataCatalogItem catalogItem = dataCatalogItemRepository.findById(request.getCatalogItemId())
                .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + request.getCatalogItemId()));

        product.setCatalogItem(catalogItem);
        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setAccessType(request.getAccessType());
        product.setBillingInterval(request.getAccessType() == ProductAccessType.ONE_TIME_PURCHASE ? BillingInterval.ONE_TIME : request.getBillingInterval());
        product.setBatchDownloadLimitMb(request.getBatchDownloadLimitMb());
        product.setMinimumPrice(request.getMinimumPrice());
        product.setIncludedSymbols(request.getIncludedSymbols());
        product.setIncludedDays(request.getIncludedDays());
        product.setPricePerAdditionalSymbol(request.getPricePerAdditionalSymbol());
        product.setPricePerAdditionalDay(request.getPricePerAdditionalDay());
        product.setFullUniverseSymbolCount(request.getFullUniverseSymbolCount());
        product.setRealtimeSubscriptionLimit(request.getRealtimeSubscriptionLimit());
        product.setMaxRealtimePayloadKb(request.getMaxRealtimePayloadKb());
        return dataProductRepository.save(product);
    }

    private CatalogItemResponse toCatalogItemResponse(DataCatalogItem item, CatalogSelectionRequest selection) {
        List<DataProduct> offers = dataProductRepository.findByCatalogItem_IdOrderByPriceAsc(item.getId()).stream()
                .map((offer) -> applyQuote(item, offer, selection))
                .sorted(Comparator.comparing((DataProduct offer) -> offer.getQuotedPrice() == null ? offer.getPrice() : offer.getQuotedPrice()))
                .toList();
        return CatalogItemResponse.from(item, offers, offers.isEmpty() ? null : offers.getFirst().getQuotedSelectionSummary());
    }

    private Optional<CatalogItemResponse> toFilteredCatalogItemResponse(
            DataCatalogItem item,
            boolean activeOnly,
            ProductAccessType accessType,
            BillingInterval billingInterval,
            CatalogSelectionRequest selection) {
        List<DataProduct> offers = dataProductRepository.findByCatalogItem_IdOrderByPriceAsc(item.getId()).stream()
                .filter((offer) -> !activeOnly || Boolean.TRUE.equals(offer.getActive()))
                .filter((offer) -> accessType == null || accessType == offer.getAccessType())
                .filter((offer) -> billingInterval == null || billingInterval == offer.getBillingInterval())
                .map((offer) -> applyQuote(item, offer, selection))
                .sorted(Comparator.comparing((DataProduct offer) -> offer.getQuotedPrice() == null ? offer.getPrice() : offer.getQuotedPrice()))
                .toList();

        if ((accessType != null || billingInterval != null) && offers.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(CatalogItemResponse.from(item, offers, offers.isEmpty() ? null : offers.getFirst().getQuotedSelectionSummary()));
    }

    private DataProduct applyQuote(DataCatalogItem item, DataProduct offer, CatalogSelectionRequest selection) {
        CatalogPricingService.CatalogPriceQuote quote = catalogPricingService.quote(item, offer, selection);
        offer.setQuotedPrice(quote.quotedPrice());
        offer.setQuotedSymbolCount(quote.quotedSymbolCount());
        offer.setQuotedDayCount(quote.quotedDayCount());
        offer.setQuotedStartDate(quote.quotedStartDate());
        offer.setQuotedEndDate(quote.quotedEndDate());
        offer.setQuotedSelectionSummary(quote.quotedSelectionSummary());
        offer.setQuotedPricingSummary(quote.quotedPricingSummary());
        return offer;
    }

    private boolean matchesSymbol(CatalogItemResponse item, String symbol) {
        if (symbol == null || symbol.isBlank() || "*".equals(symbol.trim())) {
            return true;
        }

        String normalized = symbol.trim().toLowerCase();
        return containsIgnoreCase(item.code(), normalized)
                || containsIgnoreCase(item.name(), normalized)
                || containsIgnoreCase(item.summary(), normalized)
                || containsIgnoreCase(item.description(), normalized)
                || containsIgnoreCase(item.sampleSymbols(), normalized);
    }

    private boolean matchesCoverage(CatalogItemResponse item, LocalDateTime availableFrom, LocalDateTime availableTo) {
        if (availableFrom == null && availableTo == null) {
            return true;
        }

        LocalDate requestedStart = availableFrom == null ? null : availableFrom.toLocalDate();
        LocalDate requestedEnd = availableTo == null ? null : availableTo.toLocalDate();
        LocalDate coverageStart = item.coverageStartDate();
        LocalDate coverageEnd = item.coverageEndDate();

        if (requestedEnd != null && coverageStart != null && coverageStart.isAfter(requestedEnd)) {
            return false;
        }

        if (requestedStart != null && coverageEnd != null && coverageEnd.isBefore(requestedStart)) {
            return false;
        }

        return true;
    }

    private boolean containsIgnoreCase(String source, String expectedLowercase) {
        return source != null && source.toLowerCase().contains(expectedLowercase);
    }
}
