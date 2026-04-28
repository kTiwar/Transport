package com.tms.edi.entity.address;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address_i18n")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressI18n {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "language_code", nullable = false, length = 16)
    private String languageCode;

    @Column(name = "address_text", columnDefinition = "text")
    private String addressText;
}