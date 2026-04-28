package com.tms.edi.parser;

import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.enums.FileType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Plugin interface for EDI file parsers.
 * Implement this interface and register as a Spring bean to add new format support.
 */
public interface FileParser {

    /**
     * Returns the file types this parser handles.
     */
    List<FileType> getSupportedTypes();

    /**
     * Parse the raw file stream into a flat map of field paths → values.
     * Array elements use indexed paths: /Lines[0]/ItemCode, /Lines[1]/ItemCode, etc.
     */
    List<Map<String, Object>> parse(InputStream stream, String fileName) throws Exception;

    /**
     * Analyze file structure and return a hierarchical schema tree
     * showing all possible field paths with sample values and types.
     */
    SchemaTreeDto analyzeStructure(InputStream stream, String fileName) throws Exception;

    /**
     * Extract the value at the given field path from a parsed record map.
     */
    default Object extractValue(Map<String, Object> record, String fieldPath) {
        return record.get(fieldPath);
    }
}
