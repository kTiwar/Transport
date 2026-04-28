package com.tms.edi.entity.address;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address_attributes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attr_id")
    private Long attrId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "attr_key", nullable = false, length = 128)
    private String attrKey;

    @Column(name = "attr_value", columnDefinition = "text")
    private String attrValue;
}