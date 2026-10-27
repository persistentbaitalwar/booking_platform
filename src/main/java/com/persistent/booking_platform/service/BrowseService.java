package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.domain.Movie;
import com.persistent.booking_platform.domain.MovieTranslation;
import com.persistent.booking_platform.domain.Offer;
import com.persistent.booking_platform.domain.SeatStatus;
import com.persistent.booking_platform.domain.Show;
import com.persistent.booking_platform.domain.ShowSeat;
import com.persistent.booking_platform.domain.ShowStatus;
import com.persistent.booking_platform.domain.Theatre;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.pricing.PriceBreakdown;
import com.persistent.booking_platform.pricing.PricingService;
import com.persistent.booking_platform.repository.CityRepository;
import com.persistent.booking_platform.repository.MovieRepository;
import com.persistent.booking_platform.repository.MovieTranslationRepository;
import com.persistent.booking_platform.repository.ShowRepository;
import com.persistent.booking_platform.repository.ShowSeatRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only B2C use cases: browse theatres and shows, view the seat map, get a price quote. */
@Service
@Transactional(readOnly = true)
public class BrowseService {
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MovieRepository movieRepository;
    private final CityRepository cityRepository;
    private final PricingService pricingService;
    private final MovieTranslationRepository translationRepository;
    private final Clock clock;

    public BrowseService(ShowRepository showRepository, ShowSeatRepository showSeatRepository,
                         MovieRepository movieRepository, CityRepository cityRepository,
                         PricingService pricingService, MovieTranslationRepository translationRepository,
                         Clock clock) {
        this.translationRepository = translationRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.movieRepository = movieRepository;
        this.cityRepository = cityRepository;
        this.pricingService = pricingService;
        this.clock = clock;
    }

    /**
     * Movies that have upcoming shows in a city, optionally filtered by language and genre. When a locale is given
     * (from Accept-Language), titles are returned in that language if a translation exists.
     */
    public List<MovieCard> moviesInCity(Long cityId, String language, String genre, String locale) {
        if (!cityRepository.existsById(cityId)) {
            throw ApiException.notFound("CITY_NOT_FOUND", "City " + cityId + " was not found");
        }
        List<Movie> movies = showRepository.findMoviesInCity(cityId, LocalDate.now(clock), ShowStatus.ACTIVE).stream()
                .filter(m -> language == null || language.isBlank() || m.getLanguage().equalsIgnoreCase(language.trim()))
                .filter(m -> genre == null || genre.isBlank() || m.getGenre().equalsIgnoreCase(genre.trim()))
                .toList();
        if (movies.isEmpty()) {
            return List.of();
        }
        Map<Long, String> titles = new HashMap<>();
        if (locale != null && !locale.isBlank()) {
            for (MovieTranslation t : translationRepository.findByMovieIdInAndLocale(
                    movies.stream().map(Movie::getId).toList(), locale)) {
                titles.put(t.getMovie().getId(), t.getTitle());
            }
        }
        return movies.stream()
                .map(m -> new MovieCard(m.getId(), titles.getOrDefault(m.getId(), m.getTitle()), m.getLanguage(),
                        m.getGenre(), m.getDurationMinutes()))
                .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
                .toList();
    }

    /** Theatres in a city that run the movie on the given date, with show timings, availability and offers. */
    public List<TheatreShowsResponse> findTheatres(Long movieId, Long cityId, LocalDate date) {
        if (!movieRepository.existsById(movieId)) {
            throw ApiException.notFound("MOVIE_NOT_FOUND", "Movie " + movieId + " was not found");
        }
        if (!cityRepository.existsById(cityId)) {
            throw ApiException.notFound("CITY_NOT_FOUND", "City " + cityId + " was not found");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<Show> shows = showRepository.findRunningShows(movieId, cityId, date, ShowStatus.ACTIVE).stream()
                .filter(s -> s.startDateTime().isAfter(now))
                .toList();
        if (shows.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> available = new HashMap<>();
        for (Object[] row : showSeatRepository.countByShowAndStatus(
                shows.stream().map(Show::getId).toList(), SeatStatus.AVAILABLE)) {
            available.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, List<Show>> byTheatre = new LinkedHashMap<>();
        shows.forEach(s -> byTheatre.computeIfAbsent(s.getTheatre().getId(), k -> new ArrayList<>()).add(s));

        List<TheatreShowsResponse> result = new ArrayList<>();
        for (List<Show> theatreShows : byTheatre.values()) {
            Theatre theatre = theatreShows.get(0).getTheatre();
            List<ShowSlot> slots = theatreShows.stream()
                    .map(s -> new ShowSlot(s.getId(), s.getScreen().getName(), s.getStartTime(), s.getBasePrice(),
                            available.getOrDefault(s.getId(), 0L)))
                    .toList();
            List<OfferInfo> offers = pricingService.applicableOffers(theatre).stream()
                    .map(this::toInfo).toList();
            result.add(new TheatreShowsResponse(theatre.getId(), theatre.getName(), theatre.getAddress(), slots, offers));
        }
        return result;
    }

    public List<SeatResponse> seatMap(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw ApiException.notFound("SHOW_NOT_FOUND", "Show " + showId + " was not found");
        }
        return showSeatRepository.findByShowIdOrderById(showId).stream()
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), s.getPrice(), s.getStatus())).toList();
    }

    /** Shows the customer what the booking would cost (including offers) without reserving anything. */
    public PriceBreakdown quote(Long showId, List<String> seatNumbers) {
        Show show = showRepository.findById(showId).orElseThrow(() ->
                ApiException.notFound("SHOW_NOT_FOUND", "Show " + showId + " was not found"));
        List<String> requested = seatNumbers.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();
        Map<String, ShowSeat> found = showSeatRepository.findByShowIdAndSeatNumberIn(showId, requested).stream()
                .collect(Collectors.toMap(ShowSeat::getSeatNumber, Function.identity()));
        List<String> missing = requested.stream().filter(n -> !found.containsKey(n)).toList();
        if (!missing.isEmpty()) {
            throw ApiException.notFound("SEAT_NOT_FOUND",
                    "Seat(s) " + String.join(", ", missing) + " do not exist for show " + showId);
        }
        return pricingService.price(show, requested.stream().map(found::get).toList());
    }

    private OfferInfo toInfo(Offer offer) {
        return new OfferInfo(offer.getCode(), offer.getType(), offer.getType().getDescription());
    }
}
