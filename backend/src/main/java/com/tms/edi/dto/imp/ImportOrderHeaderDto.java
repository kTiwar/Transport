package com.tms.edi.dto.imp;

import com.tms.edi.enums.ImportStatus;
import com.tms.edi.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderHeaderDto {

    private Long entryNo;
    private String communicationPartner;
    private String externalOrderNo;
    private String externalCustomerNo;
    private TransactionType transactionType;
    private ImportStatus status;
    private String tmsOrderNo;
    private String transportType;
    private String tripTypeNo;
    private String office;
    private String carrierNo;
    private String countryOfOrigin;
    private String countryOfDestination;

    private String carrierName;
    private String vesselNameImport;
    private String vesselNameExport;
    private String originInfo;
    private String destinationInfo;
    private String sealNo;
    private LocalDateTime vesselEta;
    private LocalDateTime vesselEtd;
    private String originPortName;
    private String destinationPortName;

    private String containerNumber;
    private String containerType;
    private String containerTypeIsoCode;
    private String carrierId;
    private String sealNumber;
    private String importOrExport;
    private String pickupPincode;
    private String pickupReference;
    private String dropoffPincode;
    private String dropoffReference;
    private Boolean containerCancelled;

    private String vesselName;
    private LocalDateTime closingDateTime;
    private LocalDateTime depotOutFromDateTime;
    private LocalDateTime depotInFromDateTime;
    private LocalDateTime vgmClosingDateTime;
    private BigDecimal vgmWeight;
    private String originCountry;
    private String destinationCountry;

    /** Go4IMP order date (header). */
    private LocalDateTime orderDate;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private String errorMessage;

    private List<ImportOrderLineDto> lines;
    private List<ImportOrderCargoDto> cargoItems;
    private List<ImportOrderReferenceDto> references;
    private List<ImportOrderEquipmentDto> orderEquipments;
    private List<ImportOrderCustomFieldDto> orderCustomFields;
    private List<ImportOrderRemarkDto> orderRemarks;

    /** EDI file row when this import was created from batch XML processing. */
    private Long importFileEntryNo;

    /** All TMS orders linked to this import entry (one or more when lines carry distinct external order ids). */
    private List<ImportTmsLinkDto> tmsOrdersForThisEntry;

    /** All import staging rows created from the same EDI file (batch XML), with their TMS numbers. */
    private List<ImportTmsLinkDto> importsFromSameFile;
}
