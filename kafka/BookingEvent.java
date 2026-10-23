package com.persistent.booking_platform.kafka;

import java.math.BigDecimal;
import java.util.List;

/** Message published on the ticket.events topic. Self-contained so consumers need no call back to this service. */
public record BookingEvent(String eventId, String type, Long bookingId, Long customerId, Long showId,
                           String movieTitle, String theatreName, String showStart, List<String> seatNumbers,
                           BigDecimal netAmount, String reason, String occurredAt) {

    public static final String CONFIRMED = "BOOKING_CONFIRMED";
    public static final String CANCELLED = "BOOKING_CANCELLED";
    public static final String RESCHEDULED = "BOOKING_RESCHEDULED";
    public static final String REASON_CUSTOMER_REQUEST = "CUSTOMER_REQUEST";
    public static final String REASON_SHOW_DELETED = "SHOW_DELETED";
}
