package com.tms.edi.dto.imp;

import java.util.List;

public record ImportProcessResult(String primaryTmsOrderNo, List<String> allTmsOrderNos) {

    public static ImportProcessResult empty() {
        return new ImportProcessResult("", List.of());
    }

    public boolean success() {
        return primaryTmsOrderNo != null && !primaryTmsOrderNo.isBlank();
    }
}