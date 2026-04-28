package com.tms.edi.repository;

import com.tms.edi.entity.MappingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MappingLineRepository extends JpaRepository<MappingLine, Long> {

    List<MappingLine> findByMappingHeader_MappingIdOrderBySequenceAsc(Long mappingId);
}
