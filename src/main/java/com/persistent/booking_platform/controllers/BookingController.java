package com.persistent.booking_platform.controllers;

import static com.persistent.booking_platform.model.Requests.*;
import static com.persistent.booking_platform.model.Responses.*;

import com.persistent.booking_platform.service.BookingService;
import com.persistent.booking_platform.service.BulkBookingService;
import com.persistent.booking_platform.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;
    private final BulkBookingService bulkBookingService;
    private final TicketService ticketService;

    public BookingController(BookingService bookingService, BulkBookingService bulkBookingService,
                             TicketService ticketService) {
        this.ticketService = ticketService;
        this.bookingService = bookingService;
        this.bulkBookingService = bulkBookingService;
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return bookingService.book(request, idempotencyKey);
    }

    /** QR code image (PNG) of a confirmed booking. */
    @GetMapping("/bookings/{bookingId}/qr")
    public ResponseEntity<byte[]> qrCode(@PathVariable Long bookingId) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(ticketService.qrImage(bookingId));
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse get(@PathVariable Long bookingId) {
        return bookingService.getBooking(bookingId);
    }

    @GetMapping("/customers/{customerId}/bookings")
    public List<BookingResponse> forCustomer(@PathVariable Long customerId) {
        return bookingService.listForCustomer(customerId);
    }

    @DeleteMapping("/bookings/{bookingId}")
    public BookingResponse cancel(@PathVariable Long bookingId) {
        return bookingService.cancel(bookingId);
    }

    @PutMapping("/bookings/{bookingId}/reschedule")
    public BookingResponse reschedule(@PathVariable Long bookingId, @Valid @RequestBody RescheduleRequest request) {
        return bookingService.reschedule(bookingId, request);
    }

    @PostMapping("/bookings/bulk")
    public BulkOperationResponse bulkBook(@Valid @RequestBody BulkBookingRequest request) {
        return bulkBookingService.bulkBook(request);
    }

    @PostMapping("/bookings/bulk-cancel")
    public BulkOperationResponse bulkCancel(@Valid @RequestBody BulkCancelRequest request) {
        return bulkBookingService.bulkCancel(request);
    }
}
