package com.tms.edi.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class FileEventProducer {

    public static final String TOPIC_RECEIVED   = "edi.file.received";
    public static final String TOPIC_PROCESSED  = "edi.file.processed";
    public static final String TOPIC_ERROR      = "edi.file.error";

    @Nullable
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReceived(Long entryNo) {
        try {
            if (kafkaTemplate == null) { log.debug("Kafka disabled — skipping FILE_RECEIVED for entryNo={}", entryNo); return; }
            Map<String, Object> event = Map.of("entryNo", entryNo, "event", "FILE_RECEIVED", "timestamp", Instant.now().toString());
            kafkaTemplate.send(TOPIC_RECEIVED, entryNo.toString(), event);
            log.debug("Published FILE_RECEIVED event for entryNo={}", entryNo);
        } catch (Exception ex) {
            log.warn("Kafka unavailable — FILE_RECEIVED event not published for entryNo={}: {}", entryNo, ex.getMessage());
        }
    }

    public void publishProcessed(Long entryNo, String status) {
        try {
            if (kafkaTemplate == null) { log.debug("Kafka disabled — skipping FILE_PROCESSED for entryNo={}", entryNo); return; }
            Map<String, Object> event = Map.of("entryNo", entryNo, "event", "FILE_PROCESSED", "status", status, "timestamp", Instant.now().toString());
            kafkaTemplate.send(TOPIC_PROCESSED, entryNo.toString(), event);
            log.debug("Published FILE_PROCESSED event for entryNo={} status={}", entryNo, status);
        } catch (Exception ex) {
            log.warn("Kafka unavailable — FILE_PROCESSED event not published for entryNo={}: {}", entryNo, ex.getMessage());
        }
    }

    public void publishError(Long entryNo, String errorMessage) {
        try {
            if (kafkaTemplate == null) { log.debug("Kafka disabled — skipping FILE_ERROR for entryNo={}", entryNo); return; }
            Map<String, Object> event = Map.of("entryNo", entryNo, "event", "FILE_ERROR", "error", errorMessage != null ? errorMessage : "Unknown error", "timestamp", Instant.now().toString());
            kafkaTemplate.send(TOPIC_ERROR, entryNo.toString(), event);
            log.debug("Published FILE_ERROR event for entryNo={}", entryNo);
        } catch (Exception ex) {
            log.warn("Kafka unavailable — FILE_ERROR event not published for entryNo={}: {}", entryNo, ex.getMessage());
        }
    }
}
