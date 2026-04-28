package com.tms.edi.repository.cfg;

import com.tms.edi.entity.cfg.ImportMapping;
import com.tms.edi.enums.MappingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportMappingRepository extends JpaRepository<ImportMapping, Long> {

    Optional<ImportMapping> findByCommunicationPartnerAndMappingTypeAndForeignIdAndActiveTrue(
            String communicationPartner, MappingType mappingType, String foreignId);

    @Query("""
            SELECT im
            FROM ImportMapping im
            WHERE im.communicationPartner = :communicationPartner
              AND im.mappingType = :mappingType
              AND im.active = true
              AND LOWER(TRIM(im.foreignId)) = LOWER(TRIM(:foreignId))
            """)
    Optional<ImportMapping> findActiveByNormalizedForeignId(
            @Param("communicationPartner") String communicationPartner,
            @Param("mappingType") MappingType mappingType,
            @Param("foreignId") String foreignId
    );

    @Query("""
            SELECT DISTINCT TRIM(im.foreignId)
            FROM ImportMapping im
            WHERE im.communicationPartner = :communicationPartner
              AND im.mappingType = :mappingType
              AND im.active = true
            ORDER BY TRIM(im.foreignId)
            """)
    List<String> findDistinctActiveForeignIds(
            @Param("communicationPartner") String communicationPartner,
            @Param("mappingType") MappingType mappingType
    );
}
