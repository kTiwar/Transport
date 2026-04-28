package com.tms.edi.kafka;

import com.tms.edi.service.ProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tms.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class FileEventConsumer {

    private final ProcessingService processingService;

    /**
     * Consumes FILE_RECEIVED events and triggers AUTO processing.
     * Retries 3× with exponential backoff (1s → 5s → 15s) before sending to DLQ.
     */
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 5, maxDelay = 60000),
        dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
        topics    = FileEventProducer.TOPIC_RECEIVED,
        groupId   = "edi-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFileReceived(Map<String, Object> event) {
        Long entryNo = toLong(event.get("entryNo"));
        if (entryNo == null) {
            log.error("Received FILE_RECEIVED event with null entryNo: {}", event);
            return;
        }
        log.info("Consuming FILE_RECEIVED event for entryNo={}", entryNo);
        processingService.processFile(entryNo, null);
    }

    /**
     * Dead Letter Topic handler — logs and alerts on unrecoverable messages.
     */
    @KafkaListener(
        topics  = FileEventProducer.TOPIC_RECEIVED + ".DLT",
        groupId = "edi-processor-dlt"
    )
    public void onDeadLetter(Map<String, Object> event) {
        Long entryNo = toLong(event.get("entryNo"));
        log.error("Dead Letter received for entryNo={} — manual intervention required. Event: {}",
                entryNo, event);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        try { return Long.parseLong(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
