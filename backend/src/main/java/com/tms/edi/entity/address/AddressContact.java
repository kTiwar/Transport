package com.tms.edi.entity.address;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "contact_name", length = 256)
    private String contactName;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "alternate_phone", length = 64)
    private String alternatePhone;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "is_primary_contact", nullable = false)
    @Builder.Default
    private Boolean isPrimaryContact = false;
}