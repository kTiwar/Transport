package com.tms.edi.entity.tms;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * TMS Address master data.
 * Maps to AL: "Go4TMS Address"
 */
@Entity
@Table(name = "tms_address")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsAddress {

    @Id
    @Column(name = "no", length = 20)
    private String no;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "street", length = 100)
    private String street;

    @Column(name = "number", length = 10)
    private String number;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "latitude", precision = 12, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 12, scale = 8)
    private BigDecimal longitude;

    @Column(name = "auto_inserted")
    @Builder.Default
    private Boolean autoInserted = false;
}
