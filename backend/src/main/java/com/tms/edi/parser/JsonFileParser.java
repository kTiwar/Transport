package com.tms.edi.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.enums.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonFileParser implements FileParser {

    private final ObjectMapper objectMapper;

    @Override
    public List<FileType> getSupportedTypes() {
        return List.of(FileType.JSON);
    }

    @Override
    public List<Map<String, Object>> parse(InputStream stream, String fileName) throws Exception {
        JsonNode root = objectMapper.readTree(stream);
        List<Map<String, Object>> records = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                Map<String, Object> flat = new LinkedHashMap<>();
                flattenNode(node, "", flat);
                records.add(flat);
            }
        } else {
            Map<String, Object> flat = new LinkedHashMap<>();
            flattenNode(root, "", flat);
            records.add(flat);
        }
        return records;
    }

    @Override
    public SchemaTreeDto analyzeStructure(InputStream stream, String fileName) throws Exception {
        JsonNode root = objectMapper.readTree(stream);
        JsonNode sample = root.isArray() ? root.get(0) : root;
        return buildSchemaNode(sample, "", "root");
    }

    private void flattenNode(JsonNode node, String prefix, Map<String, Object> result) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String childPath = prefix.isEmpty() ? entry.getKey() : prefix + "/" + entry.getKey();
                flattenNode(entry.getValue(), childPath, result);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flattenNode(node.get(i), prefix + "[" + i + "]", result);
            }
        } else {
            result.put(prefix, node.isNull() ? null : node.asText());
        }
    }

    private SchemaTreeDto buildSchemaNode(JsonNode node, String path, String name) {
        List<SchemaTreeDto> children = new ArrayList<>();

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String childPath = path.isEmpty() ? entry.getKey() : path + "/" + entry.getKey();
                children.add(buildSchemaNode(entry.getValue(), childPath, entry.getKey()));
            });
            return SchemaTreeDto.builder()
                    .path(path).name(name).type("OBJECT")
                    .isArray(false).children(children).build();
        } else if (node.isArray()) {
            JsonNode sample = node.size() > 0 ? node.get(0) : null;
            if (sample != null) {
                sample.fields().forEachRemaining(entry -> {
                    String childPath = path + "[*]/" + entry.getKey();
                    children.add(buildSchemaNode(entry.getValue(), childPath, entry.getKey()));
                });
            }
            return SchemaTreeDto.builder()
                    .path(path).name(name).type("ARRAY")
                    .isArray(true).arrayCount(node.size()).children(children).build();
        } else {
            return SchemaTreeDto.builder()
                    .path(path).name(name).type(inferJsonType(node))
                    .sampleValue(node.isNull() ? null : node.asText())
                    .isArray(false).children(children).build();
        }
    }

    private String inferJsonType(JsonNode node) {
        if (node.isInt() || node.isLong() || node.isDouble() || node.isFloat()) return "NUMBER";
        if (node.isBoolean()) return "BOOLEAN";
        String text = node.asText();
        if (text.matches("\\d{4}-\\d{2}-\\d{2}.*")) return "DATE";
        return "STRING";
    }
}
