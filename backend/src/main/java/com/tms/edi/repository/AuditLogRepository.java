package com.tms.edi.repository;

import com.tms.edi.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);

    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
}
