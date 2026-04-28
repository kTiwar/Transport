package com.tms.edi.repository.master;

import com.tms.edi.entity.master.MasterParty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterPartyRepository extends JpaRepository<MasterParty, Long> {

    Page<MasterParty> findByPartyTypeOrderByPartyCodeAsc(String partyType, Pageable pageable);

    Page<MasterParty> findByPartyTypeAndIsActiveOrderByPartyCodeAsc(String partyType, Boolean active, Pageable pageable);

    Optional<MasterParty> findByPartyTypeAndPartyCode(String partyType, String partyCode);
}