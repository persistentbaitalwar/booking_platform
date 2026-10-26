package com.persistent.booking_platform.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** A message for a customer produced by processing a ticket event (stand-in for email or SMS). */
@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_customer", columnList = "customer_id"))
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(Long customerId, Long bookingId, String message) {
        this.customerId = customerId;
        this.bookingId = bookingId;
        this.message = message;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getBookingId() { return bookingId; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
