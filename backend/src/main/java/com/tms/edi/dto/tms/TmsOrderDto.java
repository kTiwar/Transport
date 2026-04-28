package com.tms.edi.dto.tms;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderDto {
    private Long id;
    private String orderNo;
    private String customerNo;
    private String transportType;
    private String tripTypeNo;
    private String office;
    private String carrierNo;
    private String communicationPartner;
    private String source;
    private String status;
    private String countryOfOrigin;
    private String countryOfDestination;
    private LocalDateTime orderDate;
    private Long impEntryNo;
    /** Import external order id used as TMS partition when one import creates several TMS orders. */
    private String importExternalOrderNo;

    private List<TmsOrderLineDto> lines;
    private List<TmsOrderCargoDto> cargoItems;
    private List<TmsOrderReferenceDto> references;
}
