package com.tms.edi.repository;

import com.tms.edi.entity.MappingLearning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MappingLearningRepository extends JpaRepository<MappingLearning, Long> {

    List<MappingLearning> findByPartnerCodeAndFileType(String partnerCode, String fileType);

    List<MappingLearning> findByPartnerCodeIsNullAndFileTypeIsNull();

    Optional<MappingLearning> findByPartnerCodeAndFileTypeAndSourceFieldPathAndTargetField(
            String partnerCode, String fileType, String sourceFieldPath, String targetField);

    @Query("SELECT m FROM MappingLearning m WHERE (m.partnerCode = :partnerCode OR m.partnerCode IS NULL) " +
           "AND (m.fileType = :fileType OR m.fileType IS NULL) ORDER BY m.acceptedCount DESC")
    List<MappingLearning> findApplicable(String partnerCode, String fileType);

    @Query("SELECT m FROM MappingLearning m ORDER BY m.acceptedCount DESC")
    List<MappingLearning> findAllOrderByAcceptedCountDesc();
}