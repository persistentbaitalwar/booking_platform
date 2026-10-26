package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.dto.Responses.NotificationResponse;

import com.persistent.booking_platform.domain.Notification;
import com.persistent.booking_platform.domain.ProcessedEvent;
import com.persistent.booking_platform.event.BookingEvent;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.repository.CustomerRepository;
import com.persistent.booking_platform.repository.NotificationRepository;
import com.persistent.booking_platform.repository.ProcessedEventRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles ticket events from Kafka: de-duplicates by event id and creates the customer notification. */
@Service
public class TicketProcessingService {
    private static final Logger LOG = LoggerFactory.getLogger(TicketProcessingService.class);

    private final ProcessedEventRepository processedEvents;
    private final NotificationRepository notifications;
    private final CustomerRepository customers;

    public TicketProcessingService(ProcessedEventRepository processedEvents, NotificationRepository notifications,
                                   CustomerRepository customers) {
        this.processedEvents = processedEvents;
        this.notifications = notifications;
        this.customers = customers;
    }

    /** @return true if the event was processed now, false if it had been processed before (duplicate delivery) */
    @Transactional
    public boolean process(BookingEvent event) {
        if (processedEvents.existsByEventId(event.eventId())) {
            LOG.debug("Skipping duplicate event {}", event.eventId());
            return false;
        }
        // The unique index on event_id also stops two consumers from handling the same id at the same moment.
        processedEvents.save(new ProcessedEvent(event.eventId()));
        String message = describe(event);
        if (message == null) {
            LOG.warn("Ignoring ticket event {} of unknown type {}", event.eventId(), event.type());
            return true;
        }
        notifications.save(new Notification(event.customerId(), event.bookingId(), message));
        LOG.info("Notification for customer {}: {}", event.customerId(), message);
        return true;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> notificationsFor(Long customerId) {
        if (!customers.existsById(customerId)) {
            throw ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer " + customerId + " was not found");
        }
        return notifications.findTop50ByCustomerIdOrderByIdDesc(customerId).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getBookingId(), n.getMessage(), n.getCreatedAt().toString()))
                .toList();
    }

    private String describe(BookingEvent e) {
        String seats = String.join(", ", e.seatNumbers());
        String when = e.showStart().replace('T', ' ');
        switch (e.type()) {
            case BookingEvent.CONFIRMED:
                return "Booking #" + e.bookingId() + " is confirmed: " + e.movieTitle() + " at " + e.theatreName()
                        + ", " + when + ". Seats: " + seats + ". Amount paid: " + e.netAmount() + ".";
            case BookingEvent.CANCELLED:
                if (BookingEvent.REASON_PAYMENT_NOT_COMPLETED.equals(e.reason())) {
                    return "Booking #" + e.bookingId() + " was released because the payment was not completed. "
                            + "Nothing was charged.";
                }
                if (BookingEvent.REASON_SHOW_DELETED.equals(e.reason())) {
                    return "The theatre cancelled the show of " + e.movieTitle() + " at " + e.theatreName() + ", "
                            + when + ". Booking #" + e.bookingId() + " was cancelled and " + e.netAmount()
                            + " will be refunded.";
                }
                return "Booking #" + e.bookingId() + " was cancelled. A refund of " + e.netAmount()
                        + " will be issued.";
            case BookingEvent.RESCHEDULED:
                return "Booking #" + e.bookingId() + " was moved to " + e.movieTitle() + " at " + e.theatreName()
                        + ", " + when + ". Seats: " + seats + ".";
            default:
                return null;
        }
    }
}
