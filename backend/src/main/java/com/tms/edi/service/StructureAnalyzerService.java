package com.tms.edi.service;

import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.entity.TmsFile;
import com.tms.edi.enums.FileType;
import com.tms.edi.parser.FileParser;
import com.tms.edi.parser.ParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructureAnalyzerService {

    private final ParserRegistry parserRegistry;
    private final FileStorageService fileStorageService;

    @Cacheable(value = "fileStructure", key = "#tmsFile.entryNo")
    public SchemaTreeDto analyzeFile(TmsFile tmsFile) throws Exception {
        FileType fileType = tmsFile.getFileType();
        FileParser parser = parserRegistry.requireParser(fileType);

        byte[] content = loadContent(tmsFile);
        log.info("Analyzing structure of file {} type {}", tmsFile.getEntryNo(), fileType);

        try (ByteArrayInputStream stream = new ByteArrayInputStream(content)) {
            return parser.analyzeStructure(stream, tmsFile.getFileName());
        }
    }

    private byte[] loadContent(TmsFile tmsFile) throws Exception {
        if (tmsFile.getFileContent() != null) {
            return tmsFile.getFileContent();
        }
        return fileStorageService.load(tmsFile.getStoragePath());
    }
}
