package com.persistent.booking_platform.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Transactional outbox row. It is written in the same database transaction as the booking change, so an event exists
 * if and only if the change was committed. A relay publishes unpublished rows to Kafka afterwards.
 */
@Entity
@Table(name = "outbox_event", indexes = @Index(name = "idx_outbox_pending", columnList = "published_at, id"))
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 500)
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(String aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attempts++;
        this.lastError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 500));
    }

    public Long getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
}
