package com.tms.edi.repository;

import com.tms.edi.entity.EdiPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EdiPartnerRepository extends JpaRepository<EdiPartner, Long> {

    Optional<EdiPartner> findByPartnerCode(String partnerCode);

    Optional<EdiPartner> findByApiKey(String apiKey);

    List<EdiPartner> findByActiveTrue();
}
