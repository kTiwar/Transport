package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderCustomField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderCustomFieldRepository extends JpaRepository<ImportOrderCustomField, Long> {

    List<ImportOrderCustomField> findByEntryNoOrderByLineNoAscFieldNameAsc(Long entryNo);
}