package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Staging table for EDI order stop/action lines.
 * Maps to AL: "Go4Imp Import Order Line"
 */
@Entity
@Table(name = "imp_order_line")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderLine {

    @EmbeddedId
    private ImportOrderLineId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImportOrderHeader header;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "external_order_no", length = 80)
    private String externalOrderNo;

    @Column(name = "action_code", length = 20)
    private String actionCode;

    // Address
    @Column(name = "external_address_no", length = 50)
    private String externalAddressNo;

    @Column(name = "address_name", length = 100)
    private String addressName;

    @Column(name = "address_street", length = 100)
    private String addressStreet;

    @Column(name = "address_number", length = 10)
    private String addressNumber;

    @Column(name = "address_city", length = 50)
    private String addressCity;

    @Column(name = "address_country_code", length = 10)
    private String addressCountryCode;

    @Column(name = "address_postal_code", length = 20)
    private String addressPostalCode;

    // DateTimes
    @Column(name = "initial_datetime_from")
    private LocalDateTime initialDatetimeFrom;

    @Column(name = "initial_datetime_until")
    private LocalDateTime initialDatetimeUntil;

    @Column(name = "requested_datetime_from")
    private LocalDateTime requestedDatetimeFrom;

    @Column(name = "requested_datetime_until")
    private LocalDateTime requestedDatetimeUntil;

    @Column(name = "booked_datetime_from")
    private LocalDateTime bookedDatetimeFrom;

    @Column(name = "booked_datetime_until")
    private LocalDateTime bookedDatetimeUntil;

    @Column(name = "closing_datetime")
    private LocalDateTime closingDatetime;

    // References & misc
    @Column(name = "order_line_ref1", length = 50)
    private String orderLineRef1;

    @Column(name = "order_line_ref2", length = 50)
    private String orderLineRef2;

    @Column(name = "container_no", length = 20)
    private String containerNo;

    @Column(name = "loaded")
    private Boolean loaded;

    @Column(name = "original")
    @Builder.Default
    private Boolean original = false;

    @Column(name = "local_order_line_id")
    private Long localOrderLineId;
}
