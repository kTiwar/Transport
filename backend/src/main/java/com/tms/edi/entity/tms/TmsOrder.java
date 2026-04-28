package com.tms.edi.entity.tms;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Final TMS Order record created after EDI import processing.
 * Maps to AL: "Go4TMS Order Header"
 */
@Entity
@Table(name = "tms_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Human-readable order number, e.g. ORD-00001 */
    @Column(name = "order_no", unique = true, nullable = false, length = 20)
    private String orderNo;

    @Column(name = "customer_no", length = 20)
    private String customerNo;

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

    /** Tracks which EDI partner originally created this order */
    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "source", length = 30)
    @Builder.Default
    private String source = "ORDER_IMPORT";

    @Column(name = "web_portal_user", length = 100)
    private String webPortalUser;

    // Neutral shipment
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

    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "OPEN";

    /** Source import staging entry (header) — one or more TMS rows can share this entry (partitions). */
    @Column(name = "imp_entry_no")
    private Long impEntryNo;

    /** Sub-order / partition id from import (line or cargo external order id) when multiple TMS orders share one import entry. */
    @Column(name = "import_external_order_no", length = 80)
    private String importExternalOrderNo;

    @OneToMany(mappedBy = "tmsOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TmsOrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "tmsOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TmsOrderCargo> cargoItems = new ArrayList<>();

    @OneToMany(mappedBy = "tmsOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TmsOrderReference> references = new ArrayList<>();

    @PrePersist
    private void generateOrderNo() {
        if (this.orderNo == null) {
            this.orderNo = "ORD-" + System.currentTimeMillis();
        }
    }
}
