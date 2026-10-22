package com.persistent.booking_platform.model;

import  com.persistent.booking_platform.entity.BookingStatus;
import com.persistent.booking_platform.entity.OfferType;
import com.persistent.booking_platform.entity.SeatStatus;
import com.persistent.booking_platform.entity.ShowStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class Responses {
    private Responses() {
    }

    public record CityResponse(Long id, String name) {
    }

    public record MovieResponse(Long id, String title, String language, String genre, int durationMinutes) {
    }

    public record CustomerResponse(Long id, String name, String email) {
    }

    public record TheatreResponse(Long id, String name, String address, Long cityId, String cityName) {
    }

    public record ScreenResponse(Long id, Long theatreId, String name, int rows, int seatsPerRow, int totalSeats) {
    }

    public record OfferResponse(Long id, String code, OfferType type, String description, Long cityId, Long theatreId) {
    }

    public record OfferInfo(String code, OfferType type, String description) {
    }

    public record ShowResponse(Long id, Long movieId, String movieTitle, Long theatreId, String theatreName,
                               String screenName, LocalDate showDate, LocalTime startTime, BigDecimal basePrice,
                               ShowStatus status, long availableSeats) {
    }

    public record SeatResponse(Long seatId, String seatNumber, BigDecimal price, SeatStatus status) {
    }

    public record ShowSlot(Long showId, String screenName, LocalTime startTime, BigDecimal basePrice,
                           long availableSeats) {
    }

    public record TheatreShowsResponse(Long theatreId, String theatreName, String address, List<ShowSlot> shows,
                                       List<OfferInfo> offers) {
    }

    public record BookingResponse(Long bookingId, Long customerId, Long showId, String movieTitle,
                                  String theatreName, String screenName, LocalDate showDate, LocalTime startTime,
                                  List<String> seatNumbers, BigDecimal grossAmount, BigDecimal discountAmount,
                                  BigDecimal netAmount, List<String> appliedOffers, BookingStatus status,
                                  String qrPayload, Long movieId, Long cityId) {
    }

    public record MovieCard(Long id, String title, String language, String genre, int durationMinutes) {
    }

    public record TranslationResponse(Long movieId, String locale, String title) {
    }

    public record TicketVerification(boolean valid, String reason, BookingResponse booking) {
    }

    public record ImportItemResult(int index, boolean success, Long showId, String errorCode, String message) {
    }

    public record ImportResponse(List<ImportItemResult> results, int imported, int failed) {
    }

    public record BulkItemResult(int index, boolean success, Long bookingId, BookingStatus status, String errorCode,
                                 String message) {
    }

    public record BulkOperationResponse(List<BulkItemResult> results, int succeeded, int failed) {
    }
}
