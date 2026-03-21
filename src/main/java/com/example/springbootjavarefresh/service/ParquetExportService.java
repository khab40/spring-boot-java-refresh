package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParquetExportService {

    private static final Schema SCHEMA = new Schema.Parser().parse("""
            {
              "type": "record",
              "name": "MarketDataRecord",
              "namespace": "com.example.springbootjavarefresh.export",
              "fields": [
                {"name": "id", "type": ["null", "long"], "default": null},
                {"name": "symbol", "type": "string"},
                {"name": "price", "type": "string"},
                {"name": "volume", "type": ["null", "long"], "default": null},
                {"name": "timestamp", "type": "string"},
                {"name": "marketDate", "type": "string"},
                {"name": "dataType", "type": "string"}
              ]
            }
            """);

    public List<ExportedParquetPart> export(List<MarketData> rows, String baseName, int maxRowsPerFile) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The OTD query returned no rows.");
        }

        List<ExportedParquetPart> parts = new ArrayList<>();
        int chunkSize = Math.max(1, maxRowsPerFile);
        for (int start = 0; start < rows.size(); start += chunkSize) {
            int end = Math.min(rows.size(), start + chunkSize);
            List<MarketData> chunk = rows.subList(start, end);
            String fileName = rows.size() > chunkSize
                    ? "%s-part-%05d.parquet".formatted(baseName, parts.size() + 1)
                    : "%s.parquet".formatted(baseName);
            parts.add(writePart(chunk, fileName));
        }
        return parts;
    }

    private ExportedParquetPart writePart(List<MarketData> rows, String fileName) {
        try {
            java.nio.file.Path tempFile = Files.createTempFile("market-data-otd-", ".parquet");
            Files.deleteIfExists(tempFile);
            try (var writer = AvroParquetWriter.<GenericRecord>builder(new Path(tempFile.toUri()))
                    .withSchema(SCHEMA)
                    .withConf(new Configuration())
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .build()) {
                for (MarketData row : rows) {
                    writer.write(toRecord(row));
                }
            }

            byte[] payload = Files.readAllBytes(tempFile);
            Files.deleteIfExists(tempFile);
            return new ExportedParquetPart(fileName, payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to export OTD result set to Parquet.", exception);
        }
    }

    private GenericRecord toRecord(MarketData row) {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("id", row.getId());
        record.put("symbol", row.getSymbol());
        record.put("price", row.getPrice().toPlainString());
        record.put("volume", row.getVolume());
        record.put("timestamp", row.getTimestamp().toString());
        record.put("marketDate", row.getMarketDate().toString());
        record.put("dataType", row.getDataType() == null ? "OTHER" : row.getDataType().name());
        return record;
    }

    public record ExportedParquetPart(String fileName, byte[] payload) {}
}
