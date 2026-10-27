package com.persistent.booking_platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persistent.booking_platform.domain.Booking;
import com.persistent.booking_platform.domain.OutboxEvent;
import com.persistent.booking_platform.domain.Show;
import com.persistent.booking_platform.event.BookingEvent;
import com.persistent.booking_platform.event.EventPublisher;
import com.persistent.booking_platform.event.KafkaTopics;
import com.persistent.booking_platform.repository.OutboxEventRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Transactional outbox: record events with the business change, publish them later, at least once, in order. */
@Service
public class OutboxService {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Joins the caller's transaction: the event is committed or rolled back together with the booking. */
    @Transactional(propagation = Propagation.MANDATORY)
    public BookingEvent record(String type, Booking booking, String reason) {
        Show show = booking.getShow();
        BookingEvent event = new BookingEvent(UUID.randomUUID().toString(), type, booking.getId(),
                booking.getCustomer().getId(), show.getId(), show.getMovie().getTitle(), show.getTheatre().getName(),
                show.startDateTime().toString(), booking.seatNumberList(), booking.getNetAmount(), reason,
                clock.instant().toString());
        try {
            repository.save(new OutboxEvent(String.valueOf(booking.getId()), type, objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Ticket event could not be serialised", ex);
        }
        return event;
    }

    /**
     * Publishes up to {@code batchSize} pending events in id order. Stops at the first failure so that events of a
     * booking are never delivered out of order; the failed row is retried on the next run.
     *
     * @return number of events published
     */
    @Transactional
    public int publishPending(EventPublisher publisher, int batchSize) {
        List<OutboxEvent> pending = repository.lockPending(PageRequest.of(0, batchSize));
        int published = 0;
        for (OutboxEvent event : pending) {
            try {
                publisher.publish(KafkaTopics.TICKET_EVENTS, event.getAggregateId(), event.getPayload());
                event.markPublished(clock.instant());
                published++;
            } catch (RuntimeException ex) {
                event.recordFailure(ex.getMessage());
                LOG.warn("Could not publish outbox event {} (attempt {}): {}", event.getId(), event.getAttempts(),
                        ex.getMessage());
                break;
            }
        }
        return published;
    }
}
