package com.tms.edi.parser;

import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class XmlFileParser implements FileParser {

    /** {@code <Order1>}, {@code <Order2>}, … under {@code <Orders>}. */
    private static final Pattern NUMBERED_ORDER_TAG = Pattern.compile("^Order(\\d+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public List<FileType> getSupportedTypes() {
        return List.of(FileType.XML);
    }

    @Override
    public List<Map<String, Object>> parse(InputStream stream, String fileName) throws Exception {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(stream);
        Element root = doc.getRootElement();

        List<Element> wmsOrders = extractOrderElements(root);
        if (wmsOrders != null && !wmsOrders.isEmpty()) {
            String orderBasePath = "/" + root.getName() + "/Orders/Order";
            List<Map<String, Object>> records = new ArrayList<>(wmsOrders.size());
            for (Element orderEl : wmsOrders) {
                Map<String, Object> flatMap = new LinkedHashMap<>();
                flattenElement(orderEl, orderBasePath, flatMap);
                records.add(flatMap);
            }
            log.debug("XML {}: {} Order record(s) under {}/Orders", fileName, records.size(), root.getName());
            return records;
        }

        Map<String, Object> flatMap = new LinkedHashMap<>();
        flattenElement(root, "/" + root.getName(), flatMap);
        return List.of(flatMap);
    }

    /**
     * WMS-style batch under {@code <Orders>}:
     * <ul>
     *   <li>Repeated {@code <Order>} … {@code </Order>} (same name), or</li>
     *   <li>{@code <Order1>}, {@code <Order2>}, … {@code <OrderN>} (tag name includes index)</li>
     * </ul>
     * Each element becomes one parsed record; flattened paths use {@code /{root}/Orders/Order/...}
     * so mappings match single-order files. Returns null if neither pattern is present.
     */
    private List<Element> extractOrderElements(Element root) {
        if (root == null) {
            return null;
        }
        Element ordersEl = root.element("Orders");
        if (ordersEl == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Element> standard = ordersEl.elements("Order");
        if (standard != null && !standard.isEmpty()) {
            return standard;
        }

        List<Element> numbered = new ArrayList<>();
        for (Object node : ordersEl.elements()) {
            if (!(node instanceof Element el)) {
                continue;
            }
            if (NUMBERED_ORDER_TAG.matcher(el.getName()).matches()) {
                numbered.add(el);
            }
        }
        if (numbered.isEmpty()) {
            return null;
        }
        numbered.sort(Comparator.comparingInt(XmlFileParser::numberedOrderSequence));
        return numbered;
    }

    private static int numberedOrderSequence(Element el) {
        Matcher m = NUMBERED_ORDER_TAG.matcher(el.getName());
        return m.matches() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    @Override
    public SchemaTreeDto analyzeStructure(InputStream stream, String fileName) throws Exception {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(stream);
        Element root = doc.getRootElement();
        return buildSchemaNode(root, "/" + root.getName());
    }

    private void flattenElement(Element element, String path, Map<String, Object> result) {
        Map<String, Integer> childCounts = new LinkedHashMap<>();
        for (Element child : element.elements()) {
            childCounts.merge(child.getName(), 1, Integer::sum);
        }

        Map<String, Integer> indexTracker = new LinkedHashMap<>();
        for (Element child : element.elements()) {
            String name = child.getName();
            int count = childCounts.get(name);
            String childPath;
            if (count > 1) {
                int idx = indexTracker.getOrDefault(name, 0);
                childPath = path + "/" + name + "[" + idx + "]";
                indexTracker.put(name, idx + 1);
            } else {
                childPath = path + "/" + name;
            }

            if (child.elements().isEmpty()) {
                result.put(childPath, child.getTextTrim());
            } else {
                flattenElement(child, childPath, result);
            }
        }
    }

    private SchemaTreeDto buildSchemaNode(Element element, String path) {
        boolean hasChildren = !element.elements().isEmpty();
        String type = hasChildren ? "OBJECT" : inferType(element.getTextTrim());

        Map<String, Integer> childCounts = new LinkedHashMap<>();
        for (Element child : element.elements()) {
            childCounts.merge(child.getName(), 1, Integer::sum);
        }

        List<SchemaTreeDto> children = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element child : element.elements()) {
            String name = child.getName();
            if (seen.contains(name)) continue;
            seen.add(name);

            int count = childCounts.getOrDefault(name, 1);
            boolean isArray = count > 1;
            String childPath = isArray
                    ? path + "/" + name + "[*]"
                    : path + "/" + name;

            SchemaTreeDto childNode = buildSchemaNode(child, childPath);
            childNode.setIsArray(isArray);
            childNode.setArrayCount(count);
            children.add(childNode);
        }

        return SchemaTreeDto.builder()
                .path(path)
                .name(element.getName())
                .type(type)
                .sampleValue(hasChildren ? null : truncate(element.getTextTrim()))
                .isArray(false)
                .arrayCount(1)
                .children(children)
                .build();
    }

    private String inferType(String value) {
        if (value == null || value.isEmpty()) return "STRING";
        if (value.matches("\\d{4}-\\d{2}-\\d{2}.*") || value.matches("\\d{2}/\\d{2}/\\d{4}")) return "DATE";
        if (value.matches("-?\\d+\\.\\d+")) return "NUMBER";
        if (value.matches("-?\\d+")) return "NUMBER";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return "BOOLEAN";
        return "STRING";
    }

    private String truncate(String val) {
        if (val == null) return null;
        return val.length() > 50 ? val.substring(0, 50) + "..." : val;
    }
}
