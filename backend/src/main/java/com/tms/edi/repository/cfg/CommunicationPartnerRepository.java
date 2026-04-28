package com.tms.edi.repository.cfg;

import com.tms.edi.entity.cfg.CommunicationPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunicationPartnerRepository extends JpaRepository<CommunicationPartner, String> {
}
