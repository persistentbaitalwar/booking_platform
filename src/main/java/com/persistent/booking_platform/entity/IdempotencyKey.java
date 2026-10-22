package com.persistent.booking_platform.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Remembers which booking a client-supplied Idempotency-Key produced, so retries never double book. */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idem_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String idempotencyKey, Long bookingId) {
        this.idempotencyKey = idempotencyKey;
        this.bookingId = bookingId;
    }

    public Long getBookingId() { return bookingId; }
}
