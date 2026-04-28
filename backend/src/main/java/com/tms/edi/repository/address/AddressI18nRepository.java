package com.tms.edi.repository.address;

import com.tms.edi.entity.address.AddressI18n;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressI18nRepository extends JpaRepository<AddressI18n, Long> {
    List<AddressI18n> findByAddressIdOrderByLanguageCodeAsc(Long addressId);
}