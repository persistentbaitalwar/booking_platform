package com.persistent.booking_platform.event;

/** Port to the message broker. The Kafka adapter is the only implementation used in production. */
public interface EventPublisher {

    /** Blocks until the broker has acknowledged the message; throws {@link EventPublishException} otherwise. */
    void publish(String topic, String key, String payload);
}
