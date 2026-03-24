package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.OtdDeliveryFileResponse;
import com.example.springbootjavarefresh.dto.OtdDeliveryRequest;
import com.example.springbootjavarefresh.dto.OtdDeliveryResponse;
import com.example.springbootjavarefresh.entity.DataDelivery;
import com.example.springbootjavarefresh.entity.DataDeliveryStatus;
import com.example.springbootjavarefresh.entity.DataProduct;
import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.repository.DataDeliveryRepository;
import com.example.springbootjavarefresh.repository.DataProductRepository;
import com.example.springbootjavarefresh.repository.UserEntitlementRepository;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OtdDeliveryService {

    private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd/HHmmss");

    private final UserRepository userRepository;
    private final DataProductRepository dataProductRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final DataDeliveryRepository dataDeliveryRepository;
    private final MarketDataService marketDataService;
    private final OtdSqlQueryEngine otdSqlQueryEngine;
    private final ParquetExportService parquetExportService;
    private final ObjectStorageService objectStorageService;
    private final DataDeliveryEmailService dataDeliveryEmailService;
    private final int maxRowsPerFile;

    public OtdDeliveryService(
            UserRepository userRepository,
            DataProductRepository dataProductRepository,
            UserEntitlementRepository userEntitlementRepository,
            DataDeliveryRepository dataDeliveryRepository,
            MarketDataService marketDataService,
            OtdSqlQueryEngine otdSqlQueryEngine,
            ParquetExportService parquetExportService,
            ObjectStorageService objectStorageService,
            DataDeliveryEmailService dataDeliveryEmailService,
            @Value("${app.otd.max-rows-per-file}") int maxRowsPerFile) {
        this.userRepository = userRepository;
        this.dataProductRepository = dataProductRepository;
        this.userEntitlementRepository = userEntitlementRepository;
        this.dataDeliveryRepository = dataDeliveryRepository;
        this.marketDataService = marketDataService;
        this.otdSqlQueryEngine = otdSqlQueryEngine;
        this.parquetExportService = parquetExportService;
        this.objectStorageService = objectStorageService;
        this.dataDeliveryEmailService = dataDeliveryEmailService;
        this.maxRowsPerFile = maxRowsPerFile;
    }

    public OtdDeliveryResponse createDelivery(Long userId, OtdDeliveryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        DataProduct product = dataProductRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Data product not found: " + request.productId()));
        if (product.getAccessType() != ProductAccessType.ONE_TIME_PURCHASE) {
            throw new IllegalArgumentException("OTD delivery is supported only for one-time purchase offers.");
        }

        UserEntitlement entitlement = resolveActiveEntitlement(user.getId(), product.getId());
        List<MarketData> rows = otdSqlQueryEngine.execute(request.sql(), marketDataService.getAllMarketData());
        List<ParquetExportService.ExportedParquetPart> exportedParts = parquetExportService.export(
                rows,
                buildBaseFileName(product),
                maxRowsPerFile
        );

        long totalBytes = exportedParts.stream().mapToLong(part -> part.payload().length).sum();
        BigDecimal consumedMegabytes = toConsumedMegabytes(totalBytes);
        BigDecimal remainingBatchMb = consumeBatchAllowance(entitlement, product, consumedMegabytes);

        String deliveryKeyPrefix = buildDeliveryPrefix(user.getId(), product.getCode());
        List<StoredObjectRef> storedObjects = new ArrayList<>();
        for (ParquetExportService.ExportedParquetPart part : exportedParts) {
            String objectKey = deliveryKeyPrefix + "/" + part.fileName();
            objectStorageService.upload(objectKey, part.payload(), "application/x-parquet");
            storedObjects.add(new StoredObjectRef(objectKey, part.payload().length));
        }

        DataDelivery delivery = new DataDelivery();
        delivery.setUser(user);
        delivery.setProduct(product);
        delivery.setStatus(DataDeliveryStatus.READY);
        delivery.setSqlText(request.sql().trim());
        delivery.setObjectKeys(serializeStoredObjects(storedObjects));
        delivery.setRowCount(rows.size());
        delivery.setFileCount(exportedParts.size());
        delivery.setTotalBytes(totalBytes);
        delivery.setConsumedMegabytes(consumedMegabytes);
        DataDelivery saved = dataDeliveryRepository.save(delivery);

        OtdDeliveryResponse response = toResponse(saved, remainingBatchMb);
        dataDeliveryEmailService.sendDeliveryEmail(user, response);
        return response;
    }

    public List<OtdDeliveryResponse> getDeliveriesForUser(Long userId) {
        return dataDeliveryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(delivery -> toResponse(delivery, resolveRemainingBatchMb(delivery.getUser().getId(), delivery.getProduct())))
                .toList();
    }

    private UserEntitlement resolveActiveEntitlement(Long userId, Long productId) {
        UserEntitlement entitlement = userEntitlementRepository
                .findFirstByUserIdAndProductIdAndStatusOrderByGrantedAtDescIdDesc(userId, productId, EntitlementStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No entitlement found for this offer. Complete checkout first."));
        if (entitlement.getExpiresAt() != null && entitlement.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The selected entitlement has expired.");
        }
        return entitlement;
    }

    private BigDecimal consumeBatchAllowance(UserEntitlement entitlement, DataProduct product, BigDecimal consumedMegabytes) {
        BigDecimal allocatedLimit = allocatedBatchLimit(product, entitlement);
        BigDecimal nextUsage = entitlement.getBatchDownloadUsedMb().add(consumedMegabytes);
        if (allocatedLimit != null && nextUsage.compareTo(allocatedLimit) > 0) {
            throw new IllegalArgumentException("Batch download limit exceeded for product " + product.getCode());
        }
        entitlement.setBatchDownloadUsedMb(nextUsage);
        userEntitlementRepository.save(entitlement);
        return allocatedLimit == null ? null : allocatedLimit.subtract(nextUsage).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveRemainingBatchMb(Long userId, DataProduct product) {
        UserEntitlement entitlement = userEntitlementRepository
                .findFirstByUserIdAndProductIdAndStatusOrderByGrantedAtDescIdDesc(userId, product.getId(), EntitlementStatus.ACTIVE)
                .orElse(null);
        if (entitlement == null) {
            return null;
        }
        BigDecimal allocatedLimit = allocatedBatchLimit(product, entitlement);
        if (allocatedLimit == null) {
            return null;
        }
        return allocatedLimit.subtract(entitlement.getBatchDownloadUsedMb()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal allocatedBatchLimit(DataProduct product, UserEntitlement entitlement) {
        if (product.getBatchDownloadLimitMb() == null) {
            return null;
        }
        int purchasedUnits = entitlement.getPurchasedUnits() == null || entitlement.getPurchasedUnits() < 1
                ? 1
                : entitlement.getPurchasedUnits();
        return product.getBatchDownloadLimitMb().multiply(BigDecimal.valueOf(purchasedUnits));
    }

    private BigDecimal toConsumedMegabytes(long totalBytes) {
        BigDecimal raw = BigDecimal.valueOf(totalBytes)
                .divide(BigDecimal.valueOf(1024 * 1024), 6, RoundingMode.UP);
        BigDecimal rounded = raw.max(new BigDecimal("0.01")).setScale(2, RoundingMode.UP);
        return rounded;
    }

    private String buildBaseFileName(DataProduct product) {
        return product.getCode().toLowerCase().replaceAll("[^a-z0-9\\-]+", "-")
                + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String buildDeliveryPrefix(Long userId, String productCode) {
        return "otd/%d/%s/%s-%s".formatted(
                userId,
                LocalDateTime.now().format(KEY_DATE_FORMAT),
                productCode.toLowerCase(),
                UUID.randomUUID()
        );
    }

    private OtdDeliveryResponse toResponse(DataDelivery delivery, BigDecimal remainingBatchMb) {
        List<OtdDeliveryFileResponse> files = storedObjects(delivery).stream()
                .map(storedObject -> {
                    ObjectStorageService.SignedObjectUrl signedUrl = objectStorageService.signGetUrl(storedObject.objectKey());
                    String fileName = storedObject.objectKey().substring(storedObject.objectKey().lastIndexOf('/') + 1);
                    return new OtdDeliveryFileResponse(
                            fileName,
                            storedObject.objectKey(),
                            signedUrl.url(),
                            storedObject.sizeBytes(),
                            signedUrl.expiresAt()
                    );
                })
                .toList();

        return new OtdDeliveryResponse(
                delivery.getId(),
                delivery.getProduct().getId(),
                delivery.getProduct().getCode(),
                delivery.getProduct().getName(),
                delivery.getStatus(),
                delivery.getSqlText(),
                delivery.getRowCount(),
                delivery.getFileCount(),
                delivery.getTotalBytes(),
                delivery.getConsumedMegabytes(),
                remainingBatchMb,
                delivery.getCreatedAt(),
                files
        );
    }

    private String serializeStoredObjects(List<StoredObjectRef> storedObjects) {
        return storedObjects.stream()
                .map(storedObject -> storedObject.objectKey() + "\t" + storedObject.sizeBytes())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<StoredObjectRef> storedObjects(DataDelivery delivery) {
        return delivery.getObjectKeys() == null || delivery.getObjectKeys().isBlank()
                ? List.of()
                : List.of(delivery.getObjectKeys().split("\\n")).stream()
                .map(this::parseStoredObject)
                .toList();
    }

    private StoredObjectRef parseStoredObject(String serialized) {
        String[] parts = serialized.split("\\t", 2);
        if (parts.length < 2) {
            return new StoredObjectRef(serialized, 0L);
        }
        return new StoredObjectRef(parts[0], Long.parseLong(parts[1]));
    }

    private record StoredObjectRef(String objectKey, long sizeBytes) {}
}
