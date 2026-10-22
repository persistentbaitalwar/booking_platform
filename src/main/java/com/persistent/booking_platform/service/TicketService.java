package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.model.Responses.BookingResponse;
import static com.persistent.booking_platform.model.Responses.TicketVerification;

import com.persistent.booking_platform.entity.BookingStatus;
import com.persistent.booking_platform.exception.ApiException;

import java.util.Optional;
import org.springframework.stereotype.Service;

/** QR code image for customers and entrance verification for theatres. */
@Service
public class TicketService {
    private final BookingService bookingService;
    private final QrCodeService qrCodeService;

    public TicketService(BookingService bookingService, QrCodeService qrCodeService) {
        this.bookingService = bookingService;
        this.qrCodeService = qrCodeService;
    }

    public byte[] qrImage(Long bookingId) {
        BookingResponse booking = bookingService.getBooking(bookingId);
        if (booking.status() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("BOOKING_NOT_ACTIVE",
                    "Booking " + bookingId + " is cancelled, so it has no valid ticket");
        }
        return qrCodeService.png(booking.qrPayload());
    }

    public TicketVerification verify(String payload) {
        Optional<Long> bookingId = qrCodeService.verify(payload);
        if (bookingId.isEmpty()) {
            return new TicketVerification(false, "The ticket code is not genuine or has been altered", null);
        }
        BookingResponse booking;
        try {
            booking = bookingService.getBooking(bookingId.get());
        } catch (ApiException ex) {
            return new TicketVerification(false, "No booking exists for this ticket code", null);
        }
        if (booking.status() != BookingStatus.CONFIRMED) {
            return new TicketVerification(false, "This booking has been cancelled", booking);
        }
        return new TicketVerification(true, "Valid ticket", booking);
    }
}
