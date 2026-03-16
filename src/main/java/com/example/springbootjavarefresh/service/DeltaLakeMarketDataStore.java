package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import io.delta.tables.DeltaTable;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;

@Component
public class DeltaLakeMarketDataStore implements MarketDataStore {

    private static final StructType SCHEMA = new StructType(new StructField[]{
            new StructField("id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("symbol", DataTypes.StringType, false, Metadata.empty()),
            new StructField("price", DataTypes.createDecimalType(18, 4), false, Metadata.empty()),
            new StructField("volume", DataTypes.LongType, true, Metadata.empty()),
            new StructField("timestamp", DataTypes.TimestampType, false, Metadata.empty()),
            new StructField("marketDate", DataTypes.DateType, false, Metadata.empty()),
            new StructField("dataType", DataTypes.StringType, false, Metadata.empty())
    });

    private final String deltaPath;
    private volatile SparkSession sparkSession;

    public DeltaLakeMarketDataStore(@Value("${marketdata.delta.path:${java.io.tmpdir}/mdl-delta/market_data}") String deltaPath) {
        this.deltaPath = deltaPath;
    }

    @Override
    public List<MarketData> findAll() {
        if (!deltaTableExists()) {
            return List.of();
        }
        return readTable()
                .collectAsList()
                .stream()
                .map(this::mapRow)
                .sorted(Comparator.comparing(MarketData::getTimestamp).reversed())
                .toList();
    }

    @Override
    public Optional<MarketData> findById(Long id) {
        if (!deltaTableExists()) {
            return Optional.empty();
        }
        List<Row> rows = readTable()
                .filter(col("id").equalTo(lit(id)))
                .limit(1)
                .collectAsList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.get(0)));
    }

    @Override
    public List<MarketData> findBySymbol(String symbol) {
        if (!deltaTableExists()) {
            return List.of();
        }
        return readTable()
                .filter(col("symbol").equalTo(lit(symbol)))
                .collectAsList()
                .stream()
                .map(this::mapRow)
                .sorted(Comparator.comparing(MarketData::getTimestamp).reversed())
                .toList();
    }

    @Override
    public MarketData save(MarketData marketData) {
        marketData.normalize();
        if (marketData.getId() == null) {
            marketData.setId(generateIdentifier());
        }

        ensureParentDirectory();

        if (deltaTableExists()) {
            DeltaTable.forPath(getSparkSession(), deltaPath)
                    .delete(col("id").equalTo(lit(marketData.getId())));
        }

        Dataset<Row> frame = getSparkSession().createDataFrame(List.of(toRow(marketData)), SCHEMA);
        frame.write()
                .format("delta")
                .mode(deltaTableExists() ? "append" : "overwrite")
                .partitionBy("marketDate", "dataType")
                .save(deltaPath);

        return marketData;
    }

    @Override
    public void deleteById(Long id) {
        if (!deltaTableExists()) {
            return;
        }
        DeltaTable.forPath(getSparkSession(), deltaPath)
                .delete(col("id").equalTo(lit(id)));
    }

    private Dataset<Row> readTable() {
        return getSparkSession()
                .read()
                .format("delta")
                .load(deltaPath);
    }

    private SparkSession getSparkSession() {
        SparkSession existing = sparkSession;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (sparkSession == null) {
                sparkSession = SparkSession.builder()
                        .appName("MarketDataLakeDeltaStore")
                        .master("local[*]")
                        .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
                        .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
                        .config("spark.ui.enabled", "false")
                        .config("spark.databricks.delta.schema.autoMerge.enabled", "true")
                        .getOrCreate();
            }
            return sparkSession;
        }
    }

    private boolean deltaTableExists() {
        Path path = Path.of(deltaPath);
        return Files.exists(path) && DeltaTable.isDeltaTable(getSparkSession(), deltaPath);
    }

    private void ensureParentDirectory() {
        Path path = Path.of(deltaPath).getParent();
        if (path == null) {
            return;
        }

        try {
            Files.createDirectories(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare Delta Lake directory: " + path, ex);
        }
    }

    private long generateIdentifier() {
        long now = System.currentTimeMillis();
        return now + ThreadLocalRandom.current().nextInt(1_000);
    }

    private Row toRow(MarketData marketData) {
        BigDecimal price = marketData.getPrice().setScale(4, RoundingMode.HALF_UP);
        return RowFactory.create(
                marketData.getId(),
                marketData.getSymbol(),
                price,
                marketData.getVolume(),
                Timestamp.valueOf(marketData.getTimestamp()),
                Date.valueOf(marketData.getMarketDate()),
                marketData.getDataType().name()
        );
    }

    private MarketData mapRow(Row row) {
        MarketData marketData = new MarketData();
        marketData.setId(row.getLong(row.fieldIndex("id")));
        marketData.setSymbol(row.getString(row.fieldIndex("symbol")));
        marketData.setPrice(row.getDecimal(row.fieldIndex("price")));
        int volumeIndex = row.fieldIndex("volume");
        marketData.setVolume(row.isNullAt(volumeIndex) ? null : row.getLong(volumeIndex));
        marketData.setTimestamp(row.getTimestamp(row.fieldIndex("timestamp")).toLocalDateTime());
        marketData.setMarketDate(row.getDate(row.fieldIndex("marketDate")).toLocalDate());
        marketData.setDataType(MarketDataType.valueOf(row.getString(row.fieldIndex("dataType"))));
        return marketData;
    }
}
