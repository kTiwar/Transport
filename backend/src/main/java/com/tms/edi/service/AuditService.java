package com.tms.edi.service;

import com.tms.edi.entity.AuditLog;
import com.tms.edi.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String username, String action, String resourceType,
                    String resourceId, String details) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .username(username != null ? username : "system")
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .details(details)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to write audit log: action={} user={}", action, username, ex);
        }
    }
}
