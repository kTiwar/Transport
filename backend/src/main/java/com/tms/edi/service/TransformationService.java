package com.tms.edi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Executes field-level transformation rules.
 * Each rule is a string identifier such as "UPPER", "DATE_FORMAT", "LOOKUP", etc.
 */
@Slf4j
@Service
public class TransformationService {

    public Object apply(String rule, Object value, Map<String, Object> params) {
        if (rule == null || rule.isBlank()) return value;
        String v = value != null ? value.toString() : null;

        return switch (rule.toUpperCase()) {
            case "DIRECT"      -> value;
            case "CONSTANT"    -> params != null ? params.get("value") : value;
            case "UPPER"       -> v != null ? v.toUpperCase() : null;
            case "LOWER"       -> v != null ? v.toLowerCase() : null;
            case "TRIM"        -> v != null ? v.trim() : null;

            case "SUBSTRING"   -> applySubstring(v, params);
            case "CONCAT"      -> applyConcat(v, params);
            case "REPLACE"     -> applyReplace(v, params);

            case "DATE_FORMAT" -> applyDateFormat(v, params);
            case "DATE_NOW"    -> LocalDate.now().toString();

            case "ROUND"       -> applyRound(v, params);
            case "MATH"        -> applyMath(v, params);
            case "TO_NUMBER"   -> v != null ? new BigDecimal(v.replaceAll("[^\\d.-]", "")) : null;

            case "IF"          -> applyIf(v, params);
            case "IF_NULL"     -> v != null && !v.isBlank() ? v : (params != null ? params.get("default") : null);

            default -> {
                log.warn("Unknown transformation rule: {}", rule);
                yield value;
            }
        };
    }

    // ── Implementations ──────────────────────────────────────────────────────

    private Object applySubstring(String v, Map<String, Object> p) {
        if (v == null || p == null) return v;
        int start = toInt(p.get("start"), 0);
        int end   = toInt(p.get("end"), v.length());
        return StringUtils.substring(v, start, end);
    }

    private Object applyConcat(String v, Map<String, Object> p) {
        if (p == null) return v;
        String sep    = (String) p.getOrDefault("separator", "");
        String prefix = (String) p.getOrDefault("prefix", "");
        String suffix = (String) p.getOrDefault("suffix", "");
        return prefix + (v != null ? v : "") + suffix;
    }

    private Object applyReplace(String v, Map<String, Object> p) {
        if (v == null || p == null) return v;
        String from  = (String) p.getOrDefault("from", "");
        String to    = (String) p.getOrDefault("to", "");
        Boolean regex = (Boolean) p.getOrDefault("regex", false);
        return Boolean.TRUE.equals(regex) ? v.replaceAll(from, to) : v.replace(from, to);
    }

    private Object applyDateFormat(String v, Map<String, Object> p) {
        if (v == null || p == null) return v;
        try {
            String fromFmt = (String) p.getOrDefault("from", "dd/MM/yyyy");
            String toFmt   = (String) p.getOrDefault("to", "yyyy-MM-dd");
            LocalDate date = LocalDate.parse(v.substring(0, fromFmt.length()),
                    DateTimeFormatter.ofPattern(fromFmt));
            return date.format(DateTimeFormatter.ofPattern(toFmt));
        } catch (Exception e) {
            log.warn("DATE_FORMAT failed for value '{}': {}", v, e.getMessage());
            return v;
        }
    }

    private Object applyRound(String v, Map<String, Object> p) {
        if (v == null) return null;
        try {
            int scale = toInt(p != null ? p.get("scale") : null, 2);
            return new BigDecimal(v).setScale(scale, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return v;
        }
    }

    private Object applyMath(String v, Map<String, Object> p) {
        if (v == null || p == null) return v;
        try {
            BigDecimal val = new BigDecimal(v);
            String op  = (String) p.getOrDefault("op", "+");
            BigDecimal operand = new BigDecimal(p.getOrDefault("operand", "0").toString());
            return switch (op) {
                case "+" -> val.add(operand);
                case "-" -> val.subtract(operand);
                case "*" -> val.multiply(operand);
                case "/" -> val.divide(operand, 4, RoundingMode.HALF_UP);
                default  -> val;
            };
        } catch (Exception e) {
            return v;
        }
    }

    private Object applyIf(String v, Map<String, Object> p) {
        if (p == null) return v;
        String condition = (String) p.getOrDefault("condition", "");
        String thenVal   = (String) p.getOrDefault("then", "");
        String elseVal   = (String) p.getOrDefault("else", "");
        if (condition.isEmpty()) return v;
        return condition.equals(v) ? thenVal : elseVal;
    }

    private int toInt(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val.toString()); }
        catch (NumberFormatException e) { return def; }
    }
}
