package com.tms.edi.routing.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.edi.routing.config.RoutingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes route-planning events when {@link KafkaTemplate} is available (Kafka auto-config not excluded).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoutingKafkaPublisher {

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RoutingProperties routingProperties;

    public void publishRoutesPlanned(RoutesPlannedEvent event) {
        if (!routingProperties.isKafkaEnabled()) {
            return;
        }
        KafkaTemplate<String, String> kt = kafkaTemplate.getIfAvailable();
        if (kt == null) {
            log.debug("KafkaTemplate not available — skip publishing {}.", event.eventType());
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            kt.send(routingProperties.getKafkaTopic(), json)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka publish failed for {}: {}", event.optimizerRunId(), ex.getMessage());
                        } else {
                            log.info("Published {} to topic {} (runId={})", event.eventType(), routingProperties.getKafkaTopic(), event.optimizerRunId());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize routing event: {}", e.getMessage());
        }
    }
}
