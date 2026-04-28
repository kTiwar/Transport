package com.tms.edi.repository.address;

import com.tms.edi.entity.address.AddressAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressAuditRepository extends JpaRepository<AddressAudit, Long> {
    List<AddressAudit> findByAddressIdOrderByChangedAtDesc(Long addressId);
}