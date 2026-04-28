package com.tms.edi.service;

import com.tms.edi.dto.imp.ImportOrderCargoDto;
import com.tms.edi.dto.imp.ImportOrderHeaderDto;
import com.tms.edi.dto.imp.ImportOrderLineDto;
import com.tms.edi.dto.imp.ImportOrderReferenceDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EdiFlatRecordToImportOrderMapperTest {

    @Test
    void build_singleFlatRecord_twoOrderBranches_twoLinesDistinctExternalNosAndGlobalLineNos() {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("/Orders/OrderDate", "20240101000000");
        rec.put("/Orders/Order[0]/ExternalOrderNumber", "822110853");
        rec.put("/Orders/Order[1]/ExternalOrderNumber", "822110854");
        rec.put("/Orders/Order[0]/OrderLines/OrderLine[0]/Action", "LOAD");
        rec.put("/Orders/Order[0]/OrderLines/OrderLine[0]/AddressCountryCode", "NL");
        rec.put("/Orders/Order[1]/OrderLines/OrderLine[0]/Action", "UNLOAD");
        rec.put("/Orders/Order[1]/OrderLines/OrderLine[0]/AddressCountryCode", "DE");
        rec.put("/Orders/Order[0]/OrderLines/OrderLine[0]/OrderLineCargos/OrderLineCargo[0]/Quantity", "11");
        rec.put("/Orders/Order[1]/OrderLines/OrderLine[0]/OrderLineCargos/OrderLineCargo[0]/Quantity", "22");
        rec.put("/Orders/Order[0]/OrderCargos/OrderCargo[0]/GoodDescription", "HDR-A");
        rec.put("/Orders/Order[1]/OrderCargos/OrderCargo[0]/GoodDescription", "HDR-B");
        rec.put("/Orders/Order[0]/OrderReferences/OrderReference[0]/ReferenceCode", "REF-A");
        rec.put("/Orders/Order[1]/OrderReferences/OrderReference[0]/ReferenceCode", "REF-B");
        rec.put("/Orders/Order[0]/OrderLines/OrderLine[0]/OrderLineReferences/OrderLineReference[0]/ReferenceCode", "LR-A");
        rec.put("/Orders/Order[1]/OrderLines/OrderLine[0]/OrderLineReferences/OrderLineReference[0]/ReferenceCode", "LR-B");
        rec.put("/Orders/Order[0]/OrderEquipments/OrderEquipment[0]/MaterialType", "MT-A");
        rec.put("/Orders/Order[0]/OrderEquipments/OrderEquipment[0]/Quantity", "1");
        rec.put("/Orders/Order[1]/OrderEquipments/OrderEquipment[0]/MaterialType", "MT-B");
        rec.put("/Orders/Order[1]/OrderEquipments/OrderEquipment[0]/Quantity", "1");

        ImportOrderHeaderDto dto = EdiFlatRecordToImportOrderMapper.build(
                null, "PARTNER", "BATCH-EXT", "CUST-1", rec);

        assertNotNull(dto.getLines());
        assertEquals(2, dto.getLines().size());
        ImportOrderLineDto l1 = dto.getLines().get(0);
        ImportOrderLineDto l2 = dto.getLines().get(1);
        assertEquals("822110853", l1.getExternalOrderNo());
        assertEquals("822110854", l2.getExternalOrderNo());

        List<ImportOrderCargoDto> cargo = dto.getCargoItems();
        assertNotNull(cargo);
        Map<Integer, ImportOrderCargoDto> byOrderLineNo = cargo.stream()
                .filter(c -> c.getOrderLineNo() != null)
                .collect(Collectors.toMap(ImportOrderCargoDto::getOrderLineNo, c -> c, (a, b) -> a));
        assertEquals("11", Objects.requireNonNull(byOrderLineNo.get(1).getQuantity()).toPlainString());
        assertEquals("22", Objects.requireNonNull(byOrderLineNo.get(2).getQuantity()).toPlainString());
        assertEquals("822110853", byOrderLineNo.get(1).getExternalOrderNo());
        assertEquals("822110854", byOrderLineNo.get(2).getExternalOrderNo());

        ImportOrderCargoDto hdrA = cargo.stream()
                .filter(c -> "HDR-A".equals(c.getDescription()))
                .findFirst()
                .orElseThrow();
        assertNull(hdrA.getOrderLineNo());

        List<ImportOrderReferenceDto> refs = dto.getReferences();
        assertNotNull(refs);
        long headerRefs = refs.stream().filter(r -> r.getOrderLineNo() == 0).count();
        assertEquals(2, headerRefs);
        ImportOrderReferenceDto lineRefA = refs.stream()
                .filter(r -> "LR-A".equals(r.getReferenceCode()))
                .findFirst()
                .orElseThrow();
        ImportOrderReferenceDto lineRefB = refs.stream()
                .filter(r -> "LR-B".equals(r.getReferenceCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, lineRefA.getOrderLineNo());
        assertEquals(2, lineRefB.getOrderLineNo());

        assertNotNull(dto.getOrderEquipments());
        assertEquals(2, dto.getOrderEquipments().size());
    }
}