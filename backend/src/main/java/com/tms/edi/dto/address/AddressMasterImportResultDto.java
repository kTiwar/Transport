package com.tms.edi.dto.address;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressMasterImportResultDto {
    private int rowsRead;
    @Builder.Default
    private int inserted = 0;
    @Builder.Default
    private int updated = 0;
    @Builder.Default
    private int skipped = 0;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}