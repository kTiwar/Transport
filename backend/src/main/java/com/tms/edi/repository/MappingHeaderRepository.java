package com.tms.edi.repository;

import com.tms.edi.entity.MappingHeader;
import com.tms.edi.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MappingHeaderRepository extends JpaRepository<MappingHeader, Long> {

    Optional<MappingHeader> findByPartner_PartnerIdAndFileTypeAndActiveFlagTrue(
            Long partnerId, FileType fileType);

    List<MappingHeader> findByPartner_PartnerId(Long partnerId);

    List<MappingHeader> findByPartner_PartnerIdAndFileType(Long partnerId, FileType fileType);

    @Modifying
    @Query("""
           UPDATE MappingHeader m SET m.activeFlag = false
           WHERE m.partner.partnerId = :partnerId
             AND m.fileType = :fileType
             AND m.activeFlag = true
           """)
    void deactivateAllForPartnerAndType(
            @Param("partnerId") Long partnerId,
            @Param("fileType")  FileType fileType);

    @Query("SELECT MAX(m.version) FROM MappingHeader m WHERE m.partner.partnerId = :partnerId AND m.fileType = :fileType")
    Integer findMaxVersion(@Param("partnerId") Long partnerId, @Param("fileType") FileType fileType);
}
