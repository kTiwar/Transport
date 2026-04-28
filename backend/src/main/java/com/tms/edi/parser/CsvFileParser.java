package com.tms.edi.parser;

import com.opencsv.CSVReader;
import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class CsvFileParser implements FileParser {

    @Override
    public List<FileType> getSupportedTypes() {
        return List.of(FileType.CSV, FileType.TXT);
    }

    @Override
    public List<Map<String, Object>> parse(InputStream stream, String fileName) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String[] headers = csvReader.readNext();
            if (headers == null) return records;

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String value = i < row.length ? row[i].trim() : "";
                    record.put(headers[i].trim(), value);
                }
                records.add(record);
            }
        }
        return records;
    }

    @Override
    public SchemaTreeDto analyzeStructure(InputStream stream, String fileName) throws Exception {
        try (CSVReader csvReader = new CSVReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String[] headers = csvReader.readNext();
            String[] sample = csvReader.readNext();

            List<SchemaTreeDto> children = new ArrayList<>();
            if (headers != null) {
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim();
                    String value = (sample != null && i < sample.length) ? sample[i].trim() : "";
                    children.add(SchemaTreeDto.builder()
                            .path(header)
                            .name(header)
                            .type(inferType(value))
                            .sampleValue(value)
                            .isArray(false)
                            .children(new ArrayList<>())
                            .build());
                }
            }
            return SchemaTreeDto.builder()
                    .path("root").name("root").type("OBJECT")
                    .isArray(false).children(children).build();
        }
    }

    private String inferType(String value) {
        if (value == null || value.isEmpty()) return "STRING";
        if (value.matches("\\d{4}-\\d{2}-\\d{2}.*") || value.matches("\\d{2}/\\d{2}/\\d{4}")) return "DATE";
        if (value.matches("-?\\d+\\.\\d+") || value.matches("-?\\d+")) return "NUMBER";
        return "STRING";
    }
}
