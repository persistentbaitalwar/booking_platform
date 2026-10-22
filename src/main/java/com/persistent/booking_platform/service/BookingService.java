package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.model.Requests.BookingRequest;
import static com.persistent.booking_platform.model.Requests.RescheduleRequest;
import static com.persistent.booking_platform.model.Responses.BookingResponse;

import com.persistent.booking_platform.entity.Booking;
import com.persistent.booking_platform.entity.BookingStatus;
import com.persistent.booking_platform.entity.Customer;
import com.persistent.booking_platform.entity.IdempotencyKey;
import com.persistent.booking_platform.entity.SeatStatus;
import com.persistent.booking_platform.entity.Show;
import com.persistent.booking_platform.entity.ShowSeat;
import com.persistent.booking_platform.entity.ShowStatus;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.payment.PaymentGateway;
import com.persistent.booking_platform.pricing.PriceBreakdown;
import com.persistent.booking_platform.pricing.PricingService;
import com.persistent.booking_platform.repository.BookingRepository;
import com.persistent.booking_platform.repository.CustomerRepository;
import com.persistent.booking_platform.repository.IdempotencyKeyRepository;
import com.persistent.booking_platform.repository.ShowRepository;
import com.persistent.booking_platform.repository.ShowSeatRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking, cancellation and rescheduling. Every public method is one ACID transaction:
 * seat rows are locked (SELECT ... FOR UPDATE, in id order), validated, changed and committed together with
 * the booking row, or everything is rolled back.
 */
@Service
public class BookingService {
    public static final int MAX_SEATS_PER_BOOKING = 10;
    public static final Duration CANCELLATION_CUTOFF = Duration.ofHours(2);

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final CustomerRepository customerRepository;
    private final PricingService pricingService;
    private final PaymentGateway paymentGateway;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final QrCodeService qrCodeService;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, ShowRepository showRepository,
                          ShowSeatRepository showSeatRepository, CustomerRepository customerRepository,
                          PricingService pricingService, PaymentGateway paymentGateway,
                          IdempotencyKeyRepository idempotencyKeyRepository, QrCodeService qrCodeService,
                          Clock clock) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.qrCodeService = qrCodeService;
        this.bookingRepository = bookingRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.customerRepository = customerRepository;
        this.pricingService = pricingService;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse book(BookingRequest request) {
        return book(request, null);
    }

    /**
     * @param idempotencyKey optional client key: a retry with the same key returns the original booking instead
     *                       of booking again (protects against double clicks and timeouts around payment)
     */
    @Transactional
    public BookingResponse book(BookingRequest request, String idempotencyKey) {
        boolean keyed = idempotencyKey != null && !idempotencyKey.isBlank();
        if (keyed) {
            if (idempotencyKey.length() > 100) {
                throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY", "The Idempotency-Key must be at most 100 characters");
            }
            Booking earlier = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                    .flatMap(k -> bookingRepository.findById(k.getBookingId())).orElse(null);
            if (earlier != null) {
                if (!earlier.getCustomer().getId().equals(request.customerId())) {
                    throw ApiException.conflict("IDEMPOTENCY_KEY_REUSED",
                            "This Idempotency-Key was already used for a different customer");
                }
                return toResponse(earlier);
            }
        }
        List<String> seatNumbers = normalise(request.seatNumbers());
        Customer customer = customerRepository.findById(request.customerId()).orElseThrow(() ->
                ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer " + request.customerId() + " was not found"));
        Show show = loadBookableShow(request.showId());

        List<ShowSeat> locked = showSeatRepository.lockSeats(show.getId(), seatNumbers);
        List<ShowSeat> ordered = requireAllAvailable(show.getId(), seatNumbers, locked);

        PriceBreakdown price = pricingService.price(show, ordered);
        Booking booking = bookingRepository.save(new Booking(customer, show, price, seatNumbers));
        ordered.forEach(seat -> seat.book(booking));
        paymentGateway.charge(customer.getId(), price.net());
        if (keyed) {
            idempotencyKeyRepository.saveAndFlush(new IdempotencyKey(idempotencyKey, booking.getId()));
        }
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId) {
        return cancelInternal(bookingId, true);
    }

    @Transactional
    public BookingResponse reschedule(Long bookingId, RescheduleRequest request) {
        List<String> newSeatNumbers = normalise(request.seatNumbers());
        Booking booking = bookingRepository.findForUpdate(bookingId).orElseThrow(() -> bookingNotFound(bookingId));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("BOOKING_NOT_ACTIVE", "Booking " + bookingId + " is cancelled and cannot be rescheduled");
        }
        requireOutsideCutoff(booking.getShow(), "rescheduled");

        Show oldShow = booking.getShow();
        Show newShow = loadBookableShow(request.newShowId());
        if (newShow.getId().equals(oldShow.getId())) {
            throw ApiException.unprocessable("RESCHEDULE_SAME_SHOW", "Please choose a different show to reschedule to");
        }
        if (!newShow.getMovie().getId().equals(oldShow.getMovie().getId())) {
            throw ApiException.unprocessable("RESCHEDULE_MOVIE_MISMATCH",
                    "A booking can only be rescheduled to another show of the same movie");
        }
        List<String> oldSeatNumbers = booking.seatNumberList();
        if (newSeatNumbers.size() != oldSeatNumbers.size()) {
            throw ApiException.unprocessable("SEAT_COUNT_MISMATCH", "Rescheduling needs exactly "
                    + oldSeatNumbers.size() + " seat(s) but " + newSeatNumbers.size() + " were selected");
        }

        // One statement, one lock order: the seats we give up and the seats we want.
        List<ShowSeat> locked = showSeatRepository.lockForReschedule(bookingId, newShow.getId(), newSeatNumbers);
        List<ShowSeat> oldSeats = locked.stream()
                .filter(s -> s.getBooking() != null && s.getBooking().getId().equals(bookingId)).toList();
        List<ShowSeat> candidateNewSeats = locked.stream()
                .filter(s -> s.getShow().getId().equals(newShow.getId())).toList();
        List<ShowSeat> newSeats = requireAllAvailable(newShow.getId(), newSeatNumbers, candidateNewSeats);

        PriceBreakdown newPrice = pricingService.price(newShow, newSeats);
        BigDecimal oldNet = booking.getNetAmount();
        oldSeats.forEach(ShowSeat::release);
        newSeats.forEach(seat -> seat.book(booking));
        booking.moveTo(newShow, newPrice, newSeatNumbers);

        BigDecimal difference = newPrice.net().subtract(oldNet);
        Long customerId = booking.getCustomer().getId();
        if (difference.signum() > 0) {
            paymentGateway.charge(customerId, difference);
        } else if (difference.signum() < 0) {
            paymentGateway.refund(customerId, difference.negate());
        }
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        return toResponse(bookingRepository.findById(bookingId).orElseThrow(() -> bookingNotFound(bookingId)));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer " + customerId + " was not found");
        }
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse).toList();
    }

    /** Used when a theatre removes a show: every confirmed booking is cancelled and refunded. */
    @Transactional
    public int cancelConfirmedBookingsForShow(Long showId) {
        List<Booking> bookings = bookingRepository.findByShowIdAndStatusOrderById(showId, BookingStatus.CONFIRMED);
        bookings.forEach(b -> cancelInternal(b.getId(), false));
        return bookings.size();
    }

    private BookingResponse cancelInternal(Long bookingId, boolean enforceCutoff) {
        Booking booking = bookingRepository.findForUpdate(bookingId).orElseThrow(() -> bookingNotFound(bookingId));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("BOOKING_ALREADY_CANCELLED", "Booking " + bookingId + " is already cancelled");
        }
        if (enforceCutoff) {
            requireOutsideCutoff(booking.getShow(), "cancelled");
        }
        List<Long> seatIds = showSeatRepository.findByBookingId(bookingId).stream().map(ShowSeat::getId).toList();
        if (!seatIds.isEmpty()) {
            showSeatRepository.lockByIds(seatIds).forEach(ShowSeat::release);
        }
        booking.cancel(clock.instant());
        paymentGateway.refund(booking.getCustomer().getId(), booking.getNetAmount());
        return toResponse(booking);
    }

    private Show loadBookableShow(Long showId) {
        Show show = showRepository.findById(showId).orElseThrow(() ->
                ApiException.notFound("SHOW_NOT_FOUND", "Show " + showId + " was not found"));
        if (show.getStatus() != ShowStatus.ACTIVE) {
            throw ApiException.conflict("SHOW_CANCELLED", "This show has been cancelled");
        }
        if (!show.startDateTime().isAfter(LocalDateTime.now(clock))) {
            throw ApiException.unprocessable("SHOW_ALREADY_STARTED",
                    "Booking is closed because this show has already started");
        }
        return show;
    }

    private void requireOutsideCutoff(Show show, String action) {
        LocalDateTime deadline = show.startDateTime().minus(CANCELLATION_CUTOFF);
        if (!LocalDateTime.now(clock).isBefore(deadline)) {
            throw ApiException.unprocessable("CHANGE_WINDOW_CLOSED", "This booking can no longer be " + action
                    + " because the show starts in less than " + CANCELLATION_CUTOFF.toHours() + " hours");
        }
    }

    private List<ShowSeat> requireAllAvailable(Long showId, List<String> requested, List<ShowSeat> found) {
        Map<String, ShowSeat> byNumber = found.stream()
                .collect(Collectors.toMap(ShowSeat::getSeatNumber, Function.identity(), (a, b) -> a));
        List<String> missing = requested.stream().filter(n -> !byNumber.containsKey(n)).toList();
        if (!missing.isEmpty()) {
            throw ApiException.notFound("SEAT_NOT_FOUND",
                    "Seat(s) " + String.join(", ", missing) + " do not exist for show " + showId);
        }
        List<String> unavailable = requested.stream()
                .filter(n -> byNumber.get(n).getStatus() != SeatStatus.AVAILABLE).toList();
        if (!unavailable.isEmpty()) {
            throw ApiException.conflict("SEAT_UNAVAILABLE", "Seat(s) " + String.join(", ", unavailable)
                    + " are no longer available. Please choose different seats.");
        }
        return requested.stream().map(byNumber::get).toList();
    }

    private List<String> normalise(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw ApiException.badRequest("INVALID_SEAT_SELECTION", "Please select at least one seat");
        }
        List<String> seats = raw.stream().map(s -> s == null ? "" : s.trim().toUpperCase(Locale.ROOT)).toList();
        if (seats.stream().anyMatch(String::isEmpty)) {
            throw ApiException.badRequest("INVALID_SEAT_SELECTION", "Seat numbers cannot be blank");
        }
        if (new LinkedHashSet<>(seats).size() != seats.size()) {
            throw ApiException.badRequest("INVALID_SEAT_SELECTION", "The seat selection contains duplicate seats");
        }
        if (seats.size() > MAX_SEATS_PER_BOOKING) {
            throw ApiException.badRequest("TOO_MANY_SEATS",
                    "You can book at most " + MAX_SEATS_PER_BOOKING + " seats in a single booking");
        }
        return seats;
    }

    private ApiException bookingNotFound(Long bookingId) {
        return ApiException.notFound("BOOKING_NOT_FOUND", "Booking " + bookingId + " was not found");
    }

    private BookingResponse toResponse(Booking b) {
        Show show = b.getShow();
        return new BookingResponse(b.getId(), b.getCustomer().getId(), show.getId(), show.getMovie().getTitle(),
                show.getTheatre().getName(), show.getScreen().getName(), show.getShowDate(), show.getStartTime(),
                b.seatNumberList(), b.getGrossAmount(), b.getDiscountAmount(), b.getNetAmount(),
                b.appliedOfferList(), b.getStatus(),
                b.getStatus() == BookingStatus.CONFIRMED
                        ? qrCodeService.payload(b.getId(), show.getId(), b.seatNumberList()) : null,
                show.getMovie().getId(), show.getTheatre().getCity().getId());
    }
}
