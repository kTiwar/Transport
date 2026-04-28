package com.tms.edi.parser;

import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
public class ExcelFileParser implements FileParser {

    @Override
    public List<FileType> getSupportedTypes() {
        return List.of(FileType.EXCEL);
    }

    @Override
    public List<Map<String, Object>> parse(InputStream stream, String fileName) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(stream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return records;

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, Object> record = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    record.put(headers.get(j), cellToString(cell));
                }
                records.add(record);
            }
        }
        return records;
    }

    @Override
    public SchemaTreeDto analyzeStructure(InputStream stream, String fileName) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(stream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            Row sampleRow = sheet.getRow(1);

            List<SchemaTreeDto> children = new ArrayList<>();
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell hCell = headerRow.getCell(i);
                    Cell sCell = sampleRow != null ? sampleRow.getCell(i) : null;
                    String header = hCell != null ? hCell.getStringCellValue().trim() : "col_" + i;
                    String sample = cellToString(sCell);
                    children.add(SchemaTreeDto.builder()
                            .path(header).name(header)
                            .type(inferCellType(sCell))
                            .sampleValue(sample)
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

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case STRING  -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf(cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default      -> "";
        };
    }

    private String inferCellType(Cell cell) {
        if (cell == null) return "STRING";
        return switch (cell.getCellType()) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? "DATE" : "NUMBER";
            case BOOLEAN -> "BOOLEAN";
            default      -> "STRING";
        };
    }
}
