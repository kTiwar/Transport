package com.tms.edi.repository.address;

import com.tms.edi.entity.address.AddressContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressContactRepository extends JpaRepository<AddressContact, Long> {
    List<AddressContact> findByAddressIdOrderByContactIdAsc(Long addressId);
}