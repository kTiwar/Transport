package com.tms.edi.entity.imp;

import com.tms.edi.enums.ImportStatus;
import com.tms.edi.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Staging table for inbound EDI orders.
 * Maps to AL: "Go4IMP Import Order Header"
 */
@Entity
@Table(name = "imp_order_header")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_no")
    private Long entryNo;

    @Column(name = "communication_partner", nullable = false, length = 20)
    private String communicationPartner;

    @Column(name = "external_order_no", length = 80)
    private String externalOrderNo;

    @Column(name = "external_customer_no", length = 30)
    private String externalCustomerNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @Builder.Default
    private TransactionType transactionType = TransactionType.INSERT_ORDER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ImportStatus status = ImportStatus.RECEIVED;

    @Column(name = "tms_order_no", length = 20)
    private String tmsOrderNo;

    /** EDI file row entry_no when this import was chained from {@code tms_files} (batch XML). */
    @Column(name = "import_file_entry_no")
    private Long importFileEntryNo;

    // Header fields
    @Column(name = "transport_type", length = 20)
    private String transportType;

    @Column(name = "trip_type_no", length = 20)
    private String tripTypeNo;

    @Column(name = "office", length = 20)
    private String office;

    @Column(name = "cust_serv_responsible", length = 20)
    private String custServResponsible;

    @Column(name = "sales_responsible", length = 20)
    private String salesResponsible;

    @Column(name = "carrier_no", length = 20)
    private String carrierNo;

    @Column(name = "web_portal_user", length = 100)
    private String webPortalUser;

    @Column(name = "traction_order", length = 20)
    private String tractionOrder;

    // Neutral Shipment
    @Column(name = "neutral_shipment")
    private Boolean neutralShipment;

    @Column(name = "ns_add_name", length = 100)
    private String nsAddName;

    @Column(name = "ns_add_street", length = 100)
    private String nsAddStreet;

    @Column(name = "ns_add_city_pc", length = 50)
    private String nsAddCityPc;

    // Cash on delivery
    @Column(name = "cash_on_delivery_type", length = 20)
    private String cashOnDeliveryType;

    @Column(name = "cash_on_delivery_amount", precision = 18, scale = 2)
    private BigDecimal cashOnDeliveryAmount;

    // Country routing
    @Column(name = "country_of_origin", length = 10)
    private String countryOfOrigin;

    @Column(name = "country_of_destination", length = 10)
    private String countryOfDestination;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "vessel_name_import", length = 80)
    private String vesselNameImport;

    @Column(name = "vessel_name_export", length = 80)
    private String vesselNameExport;

    @Column(name = "origin_info", length = 100)
    private String originInfo;

    @Column(name = "destination_info", length = 100)
    private String destinationInfo;

    @Column(name = "seal_no", length = 30)
    private String sealNo;

    @Column(name = "vessel_eta")
    private LocalDateTime vesselEta;

    @Column(name = "vessel_etd")
    private LocalDateTime vesselEtd;

    @Column(name = "origin_port_name", length = 50)
    private String originPortName;

    @Column(name = "destination_port_name", length = 50)
    private String destinationPortName;

    @Column(name = "container_number", length = 50)
    private String containerNumber;

    @Column(name = "container_type", length = 20)
    private String containerType;

    @Column(name = "container_type_iso_code", length = 10)
    private String containerTypeIsoCode;

    @Column(name = "carrier_id", length = 20)
    private String carrierId;

    @Column(name = "seal_number", length = 50)
    private String sealNumber;

    @Column(name = "import_or_export", length = 20)
    private String importOrExport;

    @Column(name = "pickup_pincode", length = 50)
    private String pickupPincode;

    @Column(name = "pickup_reference", length = 150)
    private String pickupReference;

    @Column(name = "dropoff_pincode", length = 50)
    private String dropoffPincode;

    @Column(name = "dropoff_reference", length = 150)
    private String dropoffReference;

    @Column(name = "container_cancelled")
    private Boolean containerCancelled;

    @Column(name = "vessel_name", length = 100)
    private String vesselName;

    @Column(name = "closing_date_time")
    private LocalDateTime closingDateTime;

    @Column(name = "depot_out_from_date_time")
    private LocalDateTime depotOutFromDateTime;

    @Column(name = "depot_in_from_date_time")
    private LocalDateTime depotInFromDateTime;

    @Column(name = "vgm_closing_date_time")
    private LocalDateTime vgmClosingDateTime;

    @Column(name = "vgm_weight", precision = 18, scale = 4)
    private BigDecimal vgmWeight;

    @Column(name = "origin_country", length = 10)
    private String originCountry;

    @Column(name = "destination_country", length = 10)
    private String destinationCountry;

    // Dates
    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // Relationships
    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportOrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportOrderCargo> cargoItems = new ArrayList<>();

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportOrderReference> references = new ArrayList<>();

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportOrderEquipment> equipments = new ArrayList<>();

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportTransportCost> transportCosts = new ArrayList<>();
}
