package com.tms.edi.repository.address;

import com.tms.edi.entity.address.AddressAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressAttributeRepository extends JpaRepository<AddressAttribute, Long> {
    List<AddressAttribute> findByAddressIdOrderByAttrIdAsc(Long addressId);
}