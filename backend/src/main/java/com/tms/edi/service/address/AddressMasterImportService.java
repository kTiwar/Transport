package com.tms.edi.service.address;

import com.tms.edi.dto.address.AddressMasterImportResultDto;
import com.tms.edi.entity.address.AddressMaster;
import com.tms.edi.repository.address.AddressMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressMasterImportService {

    private final AddressMasterRepository addressMasterRepository;

    /**
     * First sheet, row 1 = headers. Required column: address_code (aliases below).
     * Upserts by address_code when row already exists.
     */
    @Transactional
    public AddressMasterImportResultDto importExcel(MultipartFile file) throws Exception {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int rowsRead = 0;
        List<String> errList = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                return AddressMasterImportResultDto.builder().rowsRead(0).errors(List.of("No sheet found in workbook.")).build();
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                return AddressMasterImportResultDto.builder().rowsRead(0).errors(List.of("Missing header row.")).build();
            }

            Map<String, Integer> col = new HashMap<>();
            for (int j = 0; j < header.getLastCellNum(); j++) {
                Cell c = header.getCell(j);
                if (c == null) continue;
                String key = normalizeHeader(cellToString(c));
                if (!key.isEmpty()) col.put(key, j);
            }

            int codeCol = firstCol(col,
                    "address_code", "addresscode", "code", "addr_code");
            if (codeCol < 0) {
                return AddressMasterImportResultDto.builder().rowsRead(0).errors(List.of("Missing required column: address_code (or code).")).build();
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String code = cellToString(row.getCell(codeCol)).trim();
                if (code.isEmpty()) {
                    skipped++;
                    continue;
                }
                rowsRead++;

                try {
                    Optional<AddressMaster> existing = addressMasterRepository.findByAddressCode(code);
                    AddressMaster m = existing.orElseGet(() -> AddressMaster.builder().addressCode(code).build());

                    m.setAddressType(str(row, col, "address_type", "type"));
                    m.setEntityType(str(row, col, "entity_type"));
                    m.setEntityId(lng(row, col, "entity_id"));
                    m.setAddressLine1(str(row, col, "address_line1", "line1", "street"));
                    m.setAddressLine2(str(row, col, "address_line2", "line2"));
                    m.setAddressLine3(str(row, col, "address_line3", "line3"));
                    m.setLandmark(str(row, col, "landmark"));
                    m.setCity(str(row, col, "city"));
                    m.setDistrict(str(row, col, "district"));
                    m.setStateProvince(str(row, col, "state_province", "state", "region"));
                    m.setPostalCode(str(row, col, "postal_code", "zip", "postcode"));
                    m.setCountryCode(str(row, col, "country_code"));
                    m.setCountryName(str(row, col, "country_name", "country"));
                    m.setLatitude(dbl(row, col, "latitude", "lat"));
                    m.setLongitude(dbl(row, col, "longitude", "lon", "lng"));
                    m.setTimezone(str(row, col, "timezone", "tz"));
                    Boolean p = bool(row, col, "is_primary", "primary");
                    Boolean a = bool(row, col, "is_active", "active");
                    m.setIsPrimary(p != null ? p : (m.getIsPrimary() != null ? m.getIsPrimary() : false));
                    m.setIsActive(a != null ? a : (m.getIsActive() != null ? m.getIsActive() : true));
                    m.setValidationStatus(str(row, col, "validation_status", "status"));

                    LocalDateTime now = LocalDateTime.now();
                    if (m.getCreatedAt() == null) m.setCreatedAt(now);
                    m.setUpdatedAt(now);

                    addressMasterRepository.save(m);
                    if (existing.isPresent()) updated++;
                    else inserted++;
                } catch (Exception ex) {
                    log.warn("Address import row {}: {}", i + 1, ex.getMessage());
                    errList.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }

        return AddressMasterImportResultDto.builder()
                .rowsRead(rowsRead)
                .inserted(inserted)
                .updated(updated)
                .skipped(skipped)
                .errors(errList)
                .build();
    }

    /**
     * Minimal .xlsx with the same header names the importer expects (first sheet, row 1).
     */
    public byte[] buildImportTemplate() throws Exception {
        String[] headers = {
                "address_code",
                "address_type",
                "entity_type",
                "entity_id",
                "address_line1",
                "address_line2",
                "address_line3",
                "landmark",
                "city",
                "district",
                "state_province",
                "postal_code",
                "country_code",
                "country_name",
                "latitude",
                "longitude",
                "timezone",
                "is_primary",
                "is_active",
                "validation_status",
        };
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("addresses");
            Row row = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            }
            String[][] sampleRows = {
                    {
                            "ADDR-BLR-HQ",
                            "BILLING",
                            "CUSTOMER",
                            "1001",
                            "No. 45 Richmond Road",
                            "Ashok Nagar",
                            "",
                            "Near MG Road Metro",
                            "Bengaluru",
                            "Bengaluru Urban",
                            "Karnataka",
                            "560001",
                            "IN",
                            "India",
                            "12.9716",
                            "77.5946",
                            "Asia/Kolkata",
                            "TRUE",
                            "TRUE",
                            "VALID"
                    },
                    {
                            "ADDR-DEL-WH1",
                            "SHIPPING",
                            "WAREHOUSE",
                            "2001",
                            "Plot 18 Logistics Park",
                            "NH-8",
                            "",
                            "Near Aerocity",
                            "Delhi",
                            "New Delhi",
                            "Delhi",
                            "110001",
                            "IN",
                            "India",
                            "28.6139",
                            "77.2090",
                            "Asia/Kolkata",
                            "FALSE",
                            "TRUE",
                            "VALID"
                    },
                    {
                            "ADDR-DXB-HUB",
                            "PICKUP",
                            "SUPPLIER",
                            "3001",
                            "Warehouse 12, Jebel Ali",
                            "South Zone",
                            "",
                            "Gate 4",
                            "Dubai",
                            "Jebel Ali",
                            "Dubai",
                            "00000",
                            "AE",
                            "United Arab Emirates",
                            "25.0657",
                            "55.1713",
                            "Asia/Dubai",
                            "FALSE",
                            "TRUE",
                            "PENDING"
                    }
            };
            for (int rowIdx = 0; rowIdx < sampleRows.length; rowIdx++) {
                Row sampleRow = sheet.createRow(rowIdx + 1);
                for (int colIdx = 0; colIdx < sampleRows[rowIdx].length; colIdx++) {
                    sampleRow.createCell(colIdx).setCellValue(sampleRows[rowIdx][colIdx]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static int firstCol(Map<String, Integer> col, String... keys) {
        for (String k : keys) {
            Integer i = col.get(k);
            if (i != null) return i;
        }
        return -1;
    }

    private static String normalizeHeader(String h) {
        if (h == null) return "";
        return h.toLowerCase(Locale.ROOT).trim()
                .replace('\uFEFF', ' ')
                .replace(" ", "_")
                .replace("-", "_");
    }

    private static String str(Row row, Map<String, Integer> col, String... keys) {
        int idx = firstCol(col, keys);
        if (idx < 0) return null;
        String v = cellToString(row.getCell(idx)).trim();
        return v.isEmpty() ? null : v;
    }

    private static Long lng(Row row, Map<String, Integer> col, String... keys) {
        String s = str(row, col, keys);
        if (s == null) return null;
        try {
            double d = Double.parseDouble(s.replace(",", ".").trim());
            return Math.round(d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double dbl(Row row, Map<String, Integer> col, String... keys) {
        String s = str(row, col, keys);
        if (s == null) return null;
        try {
            return Double.parseDouble(s.replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean bool(Row row, Map<String, Integer> col, String... keys) {
        String s = str(row, col, keys);
        if (s == null) return null;
        String u = s.toUpperCase(Locale.ROOT);
        if ("TRUE".equals(u) || "YES".equals(u) || "1".equals(u) || "Y".equals(u)) return true;
        if ("FALSE".equals(u) || "NO".equals(u) || "0".equals(u) || "N".equals(u)) return false;
        return null;
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf(cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default -> "";
        };
    }
}
