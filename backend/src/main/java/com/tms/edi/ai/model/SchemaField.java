package com.tms.edi.ai.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single flattened field extracted from a source schema
 * (XML, JSON, CSV, Excel).
 */
@Data
@Builder
public class SchemaField {

    /** Full path in the source document (e.g. /Orders/Order/ContainerInfo/ContainerType) */
    private String path;

    /** Leaf field name (e.g. ContainerType) */
    private String name;

    /** Inferred data type: STRING, NUMBER, DATE, BOOLEAN, UNKNOWN */
    private String type;

    /** A representative sample value from the source file */
    private String sampleValue;

    /** True if this field is inside a repeating / array element */
    private boolean array;

    /** Nesting depth in the schema tree (0 = root) */
    private int depth;
}