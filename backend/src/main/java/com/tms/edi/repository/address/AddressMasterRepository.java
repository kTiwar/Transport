package com.tms.edi.repository.address;

import com.tms.edi.entity.address.AddressMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddressMasterRepository extends JpaRepository<AddressMaster, Long> {

    Optional<AddressMaster> findByAddressCode(String addressCode);

    @Query("""
        SELECT m FROM AddressMaster m WHERE
          :q IS NULL OR :q = '' OR
          LOWER(m.addressCode) LIKE LOWER(CONCAT('%', :q, '%')) OR
          LOWER(COALESCE(m.city, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
          LOWER(COALESCE(m.postalCode, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
          LOWER(COALESCE(m.countryName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    Page<AddressMaster> search(@Param("q") String q, Pageable pageable);
}