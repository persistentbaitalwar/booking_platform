package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.dto.Requests.*;
import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.domain.City;
import com.persistent.booking_platform.domain.Movie;
import com.persistent.booking_platform.domain.Screen;
import com.persistent.booking_platform.domain.SeatStatus;
import com.persistent.booking_platform.domain.Show;
import com.persistent.booking_platform.domain.ShowSeat;
import com.persistent.booking_platform.domain.ShowStatus;
import com.persistent.booking_platform.domain.Theatre;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.repository.CityRepository;
import com.persistent.booking_platform.repository.MovieRepository;
import com.persistent.booking_platform.repository.ScreenRepository;
import com.persistent.booking_platform.repository.ShowRepository;
import com.persistent.booking_platform.repository.ShowSeatRepository;
import com.persistent.booking_platform.repository.TheatreRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** B2B use cases for theatre partners: onboarding, screens, shows and seat inventory. */
@Service
public class PartnerService {
    private final CityRepository cityRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingService bookingService;
    private final Clock clock;

    public PartnerService(CityRepository cityRepository, TheatreRepository theatreRepository,
                          ScreenRepository screenRepository, MovieRepository movieRepository,
                          ShowRepository showRepository, ShowSeatRepository showSeatRepository,
                          BookingService bookingService, Clock clock) {
        this.cityRepository = cityRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Transactional
    public TheatreResponse onboardTheatre(TheatreRequest request) {
        return onboardTheatre(request, null);
    }

    /** @param ownerUserId the partner account that owns the theatre; only the owner (or an admin) may manage it */
    @Transactional
    public TheatreResponse onboardTheatre(TheatreRequest request, Long ownerUserId) {
        City city = cityRepository.findById(request.cityId()).orElseThrow(() ->
                ApiException.notFound("CITY_NOT_FOUND", "City " + request.cityId() + " was not found"));
        String name = request.name().trim();
        if (theatreRepository.existsByCityIdAndNameIgnoreCase(city.getId(), name)) {
            throw ApiException.conflict("THEATRE_EXISTS",
                    "Theatre '" + name + "' is already onboarded in " + city.getName());
        }
        Theatre draft = new Theatre(name, request.address().trim(), city);
        draft.assignOwner(ownerUserId);
        Theatre theatre = theatreRepository.save(draft);
        return new TheatreResponse(theatre.getId(), theatre.getName(), theatre.getAddress(), city.getId(), city.getName());
    }

    @Transactional
    public ScreenResponse addScreen(Long theatreId, ScreenRequest request) {
        Theatre theatre = theatreRepository.findById(theatreId).orElseThrow(() ->
                ApiException.notFound("THEATRE_NOT_FOUND", "Theatre " + theatreId + " was not found"));
        String name = request.name().trim();
        if (screenRepository.existsByTheatreIdAndNameIgnoreCase(theatreId, name)) {
            throw ApiException.conflict("SCREEN_EXISTS", "Screen '" + name + "' already exists in this theatre");
        }
        Screen screen = screenRepository.save(new Screen(theatre, name, request.rows(), request.seatsPerRow()));
        return new ScreenResponse(screen.getId(), theatreId, screen.getName(), screen.getRows(),
                screen.getSeatsPerRow(), screen.getRows() * screen.getSeatsPerRow());
    }

    @Transactional
    public ShowResponse createShow(ShowRequest request) {
        Movie movie = movieRepository.findById(request.movieId()).orElseThrow(() ->
                ApiException.notFound("MOVIE_NOT_FOUND", "Movie " + request.movieId() + " was not found"));
        Screen screen = screenRepository.findForUpdate(request.screenId()).orElseThrow(() ->
                ApiException.notFound("SCREEN_NOT_FOUND", "Screen " + request.screenId() + " was not found"));
        requireFuture(request.showDate(), request.startTime());
        requireNoOverlap(screen.getId(), movie, request.showDate(), request.startTime(), null);

        Show show = showRepository.save(new Show(movie, screen, request.showDate(), request.startTime(), request.basePrice()));
        List<ShowSeat> seats = new ArrayList<>();
        for (int row = 0; row < screen.getRows(); row++) {
            for (int number = 1; number <= screen.getSeatsPerRow(); number++) {
                String seatNumber = String.valueOf((char) ('A' + row)) + number;
                seats.add(new ShowSeat(show, seatNumber, request.basePrice(), SeatStatus.AVAILABLE));
            }
        }
        showSeatRepository.saveAll(seats);
        return toResponse(show, seats.size());
    }

    @Transactional
    public ShowResponse updateShow(Long showId, UpdateShowRequest request) {
        Show show = loadActiveShow(showId);
        List<ShowSeat> seats = showSeatRepository.lockAllForShow(showId);

        LocalDate newDate = request.showDate() != null ? request.showDate() : show.getShowDate();
        LocalTime newTime = request.startTime() != null ? request.startTime() : show.getStartTime();
        boolean timeChanged = !newDate.equals(show.getShowDate()) || !newTime.equals(show.getStartTime());
        if (timeChanged) {
            if (seats.stream().anyMatch(s -> s.getStatus() == SeatStatus.BOOKED)) {
                throw ApiException.conflict("SHOW_HAS_BOOKINGS", "The show time cannot be changed because customers "
                        + "have already booked seats. Delete the show instead; bookings will be cancelled and refunded.");
            }
            requireFuture(newDate, newTime);
            screenRepository.findForUpdate(show.getScreen().getId());
            requireNoOverlap(show.getScreen().getId(), show.getMovie(), newDate, newTime, showId);
            show.reschedule(newDate, newTime);
        }
        if (request.basePrice() != null) {
            show.setBasePrice(request.basePrice());
            seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                    .forEach(s -> s.setPrice(request.basePrice()));
        }
        return toResponse(show, availableCount(seats));
    }

    /** Soft delete: bookings are cancelled and refunded, remaining seats are blocked, history is kept. */
    @Transactional
    public void deleteShow(Long showId) {
        Show show = loadActiveShow(showId);
        bookingService.cancelConfirmedBookingsForShow(showId);

        List<ShowSeat> seats = showSeatRepository.lockAllForShow(showId);
        show.cancel();
        // A booking may have committed between the first pass and the locks above: sweep once more.
        bookingService.cancelConfirmedBookingsForShow(showId);
        seats.stream().filter(s -> s.getStatus() != SeatStatus.BLOCKED).forEach(s -> {
            s.release();
            s.setStatus(SeatStatus.BLOCKED);
        });
    }

    @Transactional
    public List<SeatResponse> updateInventory(Long showId, SeatInventoryRequest request) {
        Show show = loadActiveShow(showId);
        Map<String, SeatSpec> specs = new HashMap<>();
        for (SeatSpec spec : request.seats()) {
            if (specs.put(spec.seatNumber(), spec) != null) {
                throw ApiException.badRequest("DUPLICATE_SEAT", "Seat " + spec.seatNumber() + " appears more than once");
            }
            if (spec.status() == SeatStatus.BOOKED) {
                throw ApiException.badRequest("INVALID_SEAT_STATUS",
                        "Seat status can only be set to AVAILABLE or BLOCKED. BOOKED is set by customer bookings.");
            }
        }
        Map<String, ShowSeat> existing = showSeatRepository.lockSeats(showId, specs.keySet()).stream()
                .collect(Collectors.toMap(ShowSeat::getSeatNumber, s -> s));
        List<String> booked = existing.values().stream().filter(s -> s.getStatus() == SeatStatus.BOOKED)
                .map(ShowSeat::getSeatNumber).sorted().toList();
        if (!booked.isEmpty()) {
            throw ApiException.conflict("SEAT_ALREADY_BOOKED", "Seat(s) " + String.join(", ", booked)
                    + " already have bookings and cannot be modified");
        }
        List<ShowSeat> created = new ArrayList<>();
        for (SeatSpec spec : specs.values()) {
            ShowSeat seat = existing.get(spec.seatNumber());
            if (seat != null) {
                if (spec.price() != null) {
                    seat.setPrice(spec.price());
                }
                if (spec.status() != null) {
                    seat.setStatus(spec.status());
                }
            } else {
                if (spec.price() == null) {
                    throw ApiException.badRequest("PRICE_REQUIRED",
                            "A price is required to add the new seat " + spec.seatNumber());
                }
                created.add(new ShowSeat(show, spec.seatNumber(), spec.price(),
                        spec.status() == null ? SeatStatus.AVAILABLE : spec.status()));
            }
        }
        showSeatRepository.saveAll(created);
        showSeatRepository.flush();
        return showSeatRepository.findByShowIdOrderById(showId).stream()
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), s.getPrice(), s.getStatus())).toList();
    }

    private Show loadActiveShow(Long showId) {
        Show show = showRepository.findById(showId).orElseThrow(() ->
                ApiException.notFound("SHOW_NOT_FOUND", "Show " + showId + " was not found"));
        if (show.getStatus() != ShowStatus.ACTIVE) {
            throw ApiException.conflict("SHOW_CANCELLED", "Show " + showId + " has already been deleted");
        }
        return show;
    }

    private void requireFuture(LocalDate date, LocalTime time) {
        if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now(clock))) {
            throw ApiException.unprocessable("INVALID_SHOW_TIME", "The show start time must be in the future");
        }
    }

    private void requireNoOverlap(Long screenId, Movie movie, LocalDate date, LocalTime time, Long ignoreShowId) {
        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes((long) movie.getDurationMinutes() + Show.TURNAROUND_MINUTES);
        List<Show> sameScreen = showRepository.findByScreenIdAndShowDateInAndStatus(screenId,
                List.of(date.minusDays(1), date, date.plusDays(1)), ShowStatus.ACTIVE);
        for (Show other : sameScreen) {
            if (other.getId().equals(ignoreShowId)) {
                continue;
            }
            if (start.isBefore(other.endDateTime()) && other.startDateTime().isBefore(end)) {
                throw ApiException.conflict("SHOW_OVERLAP", "This screen already has the show '"
                        + other.getMovie().getTitle() + "' from " + other.getStartTime() + " on "
                        + other.getShowDate() + " which overlaps with the requested time");
            }
        }
    }

    private long availableCount(List<ShowSeat> seats) {
        return seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
    }

    private ShowResponse toResponse(Show show, long available) {
        return new ShowResponse(show.getId(), show.getMovie().getId(), show.getMovie().getTitle(),
                show.getTheatre().getId(), show.getTheatre().getName(), show.getScreen().getName(),
                show.getShowDate(), show.getStartTime(), show.getBasePrice(), show.getStatus(), available);
    }
}
