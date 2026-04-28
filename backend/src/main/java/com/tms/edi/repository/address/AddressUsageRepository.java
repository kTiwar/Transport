package com.tms.edi.repository.address;

import com.tms.edi.entity.address.AddressUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressUsageRepository extends JpaRepository<AddressUsage, Long> {
    List<AddressUsage> findByAddressIdOrderByPriorityAscUsageIdAsc(Long addressId);
}