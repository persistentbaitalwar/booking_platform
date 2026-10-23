package com.persistent.booking_platform.kafka;

import com.persistent.booking_platform.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the outbox and publishes new events to Kafka. Only exists when Kafka is enabled. */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OutboxRelay {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    static final int BATCH_SIZE = 100;

    private final OutboxService outboxService;
    private final EventPublisher publisher;

    public OutboxRelay(OutboxService outboxService, EventPublisher publisher) {
        this.outboxService = outboxService;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${app.kafka.relay-delay-ms:1000}")
    public void relay() {
        int published = outboxService.publishPending(publisher, BATCH_SIZE);
        if (published > 0) {
            LOG.debug("Published {} ticket event(s) to Kafka", published);
        }
    }
}
