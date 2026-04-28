package com.tms.edi.repository.tms;

import com.tms.edi.entity.tms.TmsOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TmsOrderRepository extends JpaRepository<TmsOrder, Long> {

    Optional<TmsOrder> findByOrderNo(String orderNo);

    List<TmsOrder> findAllByImpEntryNoOrderByIdAsc(Long impEntryNo);

    Optional<TmsOrder> findFirstByImpEntryNoAndImportExternalOrderNoOrderByIdDesc(
            Long impEntryNo, String importExternalOrderNo);

    Optional<TmsOrder> findFirstByImpEntryNoAndImportExternalOrderNoIsNullOrderByIdDesc(Long impEntryNo);

    Page<TmsOrder> findAllByOrderByOrderDateDesc(Pageable pageable);

    Page<TmsOrder> findByImpEntryNo(Long impEntryNo, Pageable pageable);

    /**
     * Lookup by external order number via the reference table.
     * Equivalent to AL IsExternalOrderExistEV primary lookup.
     */
    @Query("""
            SELECT o FROM TmsOrder o
            JOIN o.references r
            WHERE r.referenceCode = :refCode
              AND r.reference     = :externalOrderNo
              AND o.customerNo    = :customerNo
            ORDER BY o.orderDate DESC
            """)
    Optional<TmsOrder> findByExternalOrderRef(
            @Param("refCode")         String refCode,
            @Param("externalOrderNo") String externalOrderNo,
            @Param("customerNo")      String customerNo);

    long countByStatus(String status);
}
