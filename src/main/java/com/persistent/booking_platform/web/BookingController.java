package com.persistent.booking_platform.web;

import static com.persistent.booking_platform.dto.Requests.*;
import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.security.AccessGuard;
import com.persistent.booking_platform.security.CurrentUser;
import com.persistent.booking_platform.service.BookingService;
import com.persistent.booking_platform.service.BulkBookingService;
import com.persistent.booking_platform.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@Tag(name = "Bookings")
public class BookingController {
    private final BookingService bookingService;
    private final BulkBookingService bulkBookingService;
    private final TicketService ticketService;
    private final AccessGuard guard;

    public BookingController(BookingService bookingService, BulkBookingService bulkBookingService,
                             TicketService ticketService, AccessGuard guard) {
        this.bookingService = bookingService;
        this.bulkBookingService = bulkBookingService;
        this.ticketService = ticketService;
        this.guard = guard;
    }

    @Operation(summary = "Book seats (send an Idempotency-Key header to make retries safe). In hold mode the booking waits for payment.")
    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                @AuthenticationPrincipal Jwt jwt) {
        guard.requireCustomer(CurrentUser.from(jwt), request.customerId());
        return bookingService.book(request, idempotencyKey);
    }

    @Operation(summary = "QR code image (PNG) of a confirmed booking")
    @GetMapping("/bookings/{bookingId}/qr")
    public ResponseEntity<byte[]> qrCode(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireBookingAccess(CurrentUser.from(jwt), bookingId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(ticketService.qrImage(bookingId));
    }

    @Operation(summary = "Get a booking")
    @GetMapping("/bookings/{bookingId}")
    public BookingResponse get(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireBookingAccess(CurrentUser.from(jwt), bookingId);
        return bookingService.getBooking(bookingId);
    }

    @Operation(summary = "List the bookings of a customer")
    @GetMapping("/customers/{customerId}/bookings")
    public List<BookingResponse> forCustomer(@PathVariable Long customerId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireCustomer(CurrentUser.from(jwt), customerId);
        return bookingService.listForCustomer(customerId);
    }

    @Operation(summary = "Cancel a booking and refund it")
    @DeleteMapping("/bookings/{bookingId}")
    public BookingResponse cancel(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireBookingAccess(CurrentUser.from(jwt), bookingId);
        return bookingService.cancel(bookingId);
    }

    @Operation(summary = "Reschedule a booking to another show of the same movie")
    @PutMapping("/bookings/{bookingId}/reschedule")
    public BookingResponse reschedule(@PathVariable Long bookingId, @Valid @RequestBody RescheduleRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        guard.requireBookingAccess(CurrentUser.from(jwt), bookingId);
        return bookingService.reschedule(bookingId, request);
    }

    @Operation(summary = "Bulk booking (partial success or all-or-nothing)")
    @PostMapping("/bookings/bulk")
    public BulkOperationResponse bulkBook(@Valid @RequestBody BulkBookingRequest request,
                                          @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = CurrentUser.from(jwt);
        request.bookings().forEach(b -> guard.requireCustomer(user, b.customerId()));
        return bulkBookingService.bulkBook(request);
    }

    @Operation(summary = "Bulk cancellation (partial success or all-or-nothing)")
    @PostMapping("/bookings/bulk-cancel")
    public BulkOperationResponse bulkCancel(@Valid @RequestBody BulkCancelRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = CurrentUser.from(jwt);
        request.bookingIds().forEach(id -> guard.requireBookingAccess(user, id));
        return bulkBookingService.bulkCancel(request);
    }
}
