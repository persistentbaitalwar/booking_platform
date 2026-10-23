package com.persistent.booking_platform.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {
    /** Six partitions: up to six consumers of one group work in parallel. Use replicas=3 on a real cluster (MSK). */
    private static final int PARTITIONS = 6;

    @Bean
    public NewTopic ticketEventsTopic() {
        return TopicBuilder.name(KafkaTopics.TICKET_EVENTS).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic ticketEventsDeadLetterTopic() {
        return TopicBuilder.name(KafkaTopics.TICKET_EVENTS_DLT).partitions(PARTITIONS).replicas(1).build();
    }

    /** Retry a failing message three times, one second apart, then park it on the dead-letter topic. */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3L));
    }
}
