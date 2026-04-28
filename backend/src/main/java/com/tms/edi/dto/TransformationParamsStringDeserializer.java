package com.tms.edi.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Accepts transformation params as either a JSON string (DB round-trip) or a structured
 * JSON object/array (UI editors). Prevents HttpMessageNotReadableException on save.
 */
public class TransformationParamsStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == null || t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t == JsonToken.VALUE_STRING) {
            return p.getText();
        }
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString();
    }
}
