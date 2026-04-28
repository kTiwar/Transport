package com.tms.edi.entity.address;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "usage_type", length = 64)
    private String usageType;

    @Column(name = "priority")
    private Integer priority;
}