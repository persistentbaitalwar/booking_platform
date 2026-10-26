package com.persistent.booking_platform.security;

import com.persistent.booking_platform.domain.Booking;
import com.persistent.booking_platform.domain.Screen;
import com.persistent.booking_platform.domain.Show;
import com.persistent.booking_platform.domain.Theatre;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.repository.BookingRepository;
import com.persistent.booking_platform.repository.ScreenRepository;
import com.persistent.booking_platform.repository.ShowRepository;
import com.persistent.booking_platform.repository.TheatreRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Object-level authorization (OWASP A01, "broken access control"): a customer only sees their own bookings and a
 * partner only manages their own theatres. A resource that belongs to somebody else is reported as "not found",
 * so ids cannot be probed. Resources that do not exist at all are left to the service, which answers 404.
 */
@Component
@Transactional(readOnly = true)
public class AccessGuard {
    private final BookingRepository bookings;
    private final ShowRepository shows;
    private final ScreenRepository screens;
    private final TheatreRepository theatres;

    public AccessGuard(BookingRepository bookings, ShowRepository shows, ScreenRepository screens,
                       TheatreRepository theatres) {
        this.bookings = bookings;
        this.shows = shows;
        this.screens = screens;
        this.theatres = theatres;
    }

    public void requireCustomer(CurrentUser user, Long customerId) {
        if (!user.isAdmin() && !Objects.equals(user.customerId(), customerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You can only act for your own customer account.");
        }
    }

    public void requireBookingAccess(CurrentUser user, Long bookingId) {
        if (user.isAdmin()) {
            return;
        }
        Optional<Booking> booking = bookings.findById(bookingId);
        if (booking.isPresent() && !Objects.equals(booking.get().getCustomer().getId(), user.customerId())) {
            throw ApiException.notFound("BOOKING_NOT_FOUND", "Booking " + bookingId + " was not found");
        }
    }

    public void requireTheatreAccess(CurrentUser user, Long theatreId) {
        if (user.isAdmin()) {
            return;
        }
        Optional<Theatre> theatre = theatres.findById(theatreId);
        if (theatre.isPresent() && !ownedBy(theatre.get(), user)) {
            throw ApiException.notFound("THEATRE_NOT_FOUND", "Theatre " + theatreId + " was not found");
        }
    }

    public void requireScreenAccess(CurrentUser user, Long screenId) {
        if (user.isAdmin()) {
            return;
        }
        Optional<Screen> screen = screens.findById(screenId);
        if (screen.isPresent() && !ownedBy(screen.get().getTheatre(), user)) {
            throw ApiException.notFound("SCREEN_NOT_FOUND", "Screen " + screenId + " was not found");
        }
    }

    public void requireShowAccess(CurrentUser user, Long showId) {
        if (user.isAdmin()) {
            return;
        }
        Optional<Show> show = shows.findById(showId);
        if (show.isPresent() && !ownedBy(show.get().getTheatre(), user)) {
            throw ApiException.notFound("SHOW_NOT_FOUND", "Show " + showId + " was not found");
        }
    }

    public boolean canManageShow(CurrentUser user, Long showId) {
        if (user.isAdmin()) {
            return true;
        }
        return shows.findById(showId).map(s -> ownedBy(s.getTheatre(), user)).orElse(false);
    }

    private boolean ownedBy(Theatre theatre, CurrentUser user) {
        return theatre.getOwnerUserId() != null && theatre.getOwnerUserId().equals(user.userId());
    }
}
