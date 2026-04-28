package com.tms.edi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Stores raw EDI files on the local filesystem (or swap for MinIO/S3 client).
 * Path pattern: {baseDir}/{partner}/{date}/{uuid}-{filename}
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${tms.storage.base-dir:./edi-files}")
    private String baseDir;

    public StorageResult store(String partnerCode, String fileName, byte[] content) throws IOException {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        String relativePath = partnerCode + "/" + date + "/" + uuid + "-" + safeName;
        Path fullPath = Path.of(baseDir, relativePath);

        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, content);

        String checksum = DigestUtils.sha256Hex(content);
        log.info("Stored file {} → {} ({} bytes)", fileName, relativePath, content.length);
        return new StorageResult(relativePath, checksum, (long) content.length);
    }

    public byte[] load(String storagePath) throws IOException {
        return Files.readAllBytes(Path.of(baseDir, storagePath));
    }

    public void delete(String storagePath) throws IOException {
        Files.deleteIfExists(Path.of(baseDir, storagePath));
    }

    public record StorageResult(String storagePath, String checksum, long fileSize) {}
}
