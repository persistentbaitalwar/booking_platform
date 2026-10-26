package com.persistent.booking_platform.dto;

import com.persistent.booking_platform.domain.OfferType;
import com.persistent.booking_platform.domain.SeatStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class Requests {
    private Requests() {
    }

    public record CityRequest(
            @NotBlank(message = "City name is required") @Size(max = 100, message = "City name must be at most 100 characters") String name) {
    }

    public record MovieRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title must be at most 200 characters") String title,
            @NotBlank(message = "Language is required") @Size(max = 50, message = "Language must be at most 50 characters") String language,
            @NotBlank(message = "Genre is required") @Size(max = 50, message = "Genre must be at most 50 characters") String genre,
            @NotNull(message = "Duration is required") @Min(value = 1, message = "Duration must be at least 1 minute")
            @Max(value = 600, message = "Duration must be at most 600 minutes") Integer durationMinutes) {
    }

    public record CustomerRequest(
            @NotBlank(message = "Name is required") @Size(max = 150, message = "Name must be at most 150 characters") String name,
            @NotBlank(message = "Email is required") @Email(message = "Email address is not valid") String email) {
    }

    public record TheatreRequest(
            @NotBlank(message = "Theatre name is required") @Size(max = 150, message = "Theatre name must be at most 150 characters") String name,
            @NotBlank(message = "Address is required") @Size(max = 300, message = "Address must be at most 300 characters") String address,
            @NotNull(message = "City is required") Long cityId) {
    }

    public record ScreenRequest(
            @NotBlank(message = "Screen name is required") @Size(max = 100, message = "Screen name must be at most 100 characters") String name,
            @NotNull(message = "Number of rows is required") @Min(value = 1, message = "A screen needs at least 1 row")
            @Max(value = 26, message = "A screen can have at most 26 rows") Integer rows,
            @NotNull(message = "Seats per row is required") @Min(value = 1, message = "A row needs at least 1 seat")
            @Max(value = 50, message = "A row can have at most 50 seats") Integer seatsPerRow) {
    }

    public record ShowRequest(
            @NotNull(message = "Movie is required") Long movieId,
            @NotNull(message = "Screen is required") Long screenId,
            @NotNull(message = "Show date is required") LocalDate showDate,
            @NotNull(message = "Start time is required") LocalTime startTime,
            @NotNull(message = "Base price is required") @DecimalMin(value = "0.01", message = "Base price must be greater than zero") BigDecimal basePrice) {
    }

    public record UpdateShowRequest(
            LocalDate showDate,
            LocalTime startTime,
            @DecimalMin(value = "0.01", message = "Base price must be greater than zero") BigDecimal basePrice) {
    }

    public record SeatSpec(
            @NotBlank(message = "Seat number is required")
            @Pattern(regexp = "^[A-Z][0-9]{1,2}$", message = "Seat number must look like A1 or B12") String seatNumber,
            @DecimalMin(value = "0.00", message = "Seat price cannot be negative") BigDecimal price,
            SeatStatus status) {
    }

    public record SeatInventoryRequest(
            @NotEmpty(message = "At least one seat is required") @Size(max = 500, message = "At most 500 seats can be updated at once")
            List<@Valid SeatSpec> seats) {
    }

    public record TranslationRequest(
            @NotBlank(message = "Locale is required")
            @Pattern(regexp = "^[a-z]{2,3}$", message = "Locale must be a language code such as hi or mr") String locale,
            @NotBlank(message = "Translated title is required") @Size(max = 200, message = "Title must be at most 200 characters") String title) {
    }

    public record ShowImportRequest(
            @NotEmpty(message = "At least one show is required") @Size(max = 200, message = "At most 200 shows can be imported at once")
            List<@Valid @NotNull(message = "Show entry cannot be null") ShowRequest> shows) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 150, message = "Name must be at most 150 characters") String name,
            @NotBlank(message = "Email is required") @Email(message = "Email address is not valid") @Size(max = 200, message = "Email must be at most 200 characters") String email,
            @NotBlank(message = "Password is required")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                    message = "Password must be 8 to 72 characters and contain at least one letter and one digit") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record OfferRequest(
            @NotBlank(message = "Offer code is required") @Size(max = 50, message = "Offer code must be at most 50 characters") String code,
            @NotNull(message = "Offer type is required") OfferType type,
            Long cityId,
            Long theatreId) {
    }

    public record BookingRequest(
            @NotNull(message = "Customer is required") Long customerId,
            @NotNull(message = "Show is required") Long showId,
            @NotEmpty(message = "Please select at least one seat") List<@NotBlank(message = "Seat number cannot be blank") String> seatNumbers) {
    }

    public record RescheduleRequest(
            @NotNull(message = "New show is required") Long newShowId,
            @NotEmpty(message = "Please select at least one seat") List<@NotBlank(message = "Seat number cannot be blank") String> seatNumbers) {
    }

    public record BulkBookingRequest(
            @NotEmpty(message = "At least one booking is required") @Size(max = 50, message = "At most 50 bookings can be sent at once")
            List<@Valid @NotNull(message = "Booking entry cannot be null") BookingRequest> bookings,
            boolean allOrNothing) {
    }

    public record BulkCancelRequest(
            @NotEmpty(message = "At least one booking id is required") @Size(max = 50, message = "At most 50 cancellations can be sent at once")
            List<@NotNull(message = "Booking id cannot be null") Long> bookingIds,
            boolean allOrNothing) {
    }
}
