package com.persistent.booking_platform.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Event ids already handled by the consumer; makes at-least-once delivery behave like exactly-once. */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() { return eventId; }
}
