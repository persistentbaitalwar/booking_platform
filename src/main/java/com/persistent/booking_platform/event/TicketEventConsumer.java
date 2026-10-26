package com.persistent.booking_platform.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persistent.booking_platform.service.TicketProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Real-time ticket processing. Several instances in the same consumer group share the partitions; a message that
 * keeps failing is retried and then moved to the dead-letter topic by the error handler.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TicketEventConsumer {
    private final ObjectMapper objectMapper;
    private final TicketProcessingService processingService;

    public TicketEventConsumer(ObjectMapper objectMapper, TicketProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @KafkaListener(topics = KafkaTopics.TICKET_EVENTS, concurrency = "3")
    public void onMessage(String payload) {
        try {
            processingService.process(objectMapper.readValue(payload, BookingEvent.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unreadable ticket event: " + ex.getOriginalMessage(), ex);
        }
    }
}
