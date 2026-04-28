package com.tms.edi.parser;

import com.tms.edi.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Auto-discovers all FileParser beans and routes by FileType.
 * New formats require only a new FileParser implementation — no changes here.
 */
@Slf4j
@Component
public class ParserRegistry {

    private final Map<FileType, FileParser> registry = new HashMap<>();

    public ParserRegistry(List<FileParser> parsers) {
        for (FileParser parser : parsers) {
            for (FileType type : parser.getSupportedTypes()) {
                registry.put(type, parser);
                log.info("Registered parser {} for type {}", parser.getClass().getSimpleName(), type);
            }
        }
    }

    public Optional<FileParser> getParser(FileType fileType) {
        return Optional.ofNullable(registry.get(fileType));
    }

    public FileParser requireParser(FileType fileType) {
        return getParser(fileType)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No parser registered for file type: " + fileType));
    }
}
