package com.persistent.booking_platform.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.pricing.PriceBreakdown;
import com.persistent.booking_platform.service.BrowseService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Browse")
@RestController
@RequestMapping("/api/v1")
public class BrowseController {
    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    /** Movies running in a city; filter with ?language=&genre=; titles follow the Accept-Language header. */
    @Operation(summary = "List movies running in a city (filter by language and genre; titles follow Accept-Language)")
    @GetMapping("/cities/{cityId}/movies")
    public List<MovieCard> moviesInCity(@PathVariable Long cityId,
                                        @RequestParam(required = false) String language,
                                        @RequestParam(required = false) String genre,
                                        @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return browseService.moviesInCity(cityId, language, genre, primaryLanguage(acceptLanguage));
    }

    private static String primaryLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        String first = acceptLanguage.split(",")[0].split(";")[0].trim();
        String language = java.util.Locale.forLanguageTag(first).getLanguage();
        return language.isEmpty() ? null : language;
    }

    /** Theatres running the movie in a city on a date, e.g. /movies/1/theatres?cityId=1&date=2026-11-01 */
    @Operation(summary = "List theatres running a movie in a city on a date, with show times, free seats and offers")
    @GetMapping("/movies/{movieId}/theatres")
    public List<TheatreShowsResponse> theatres(@PathVariable Long movieId, @RequestParam Long cityId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return browseService.findTheatres(movieId, cityId, date);
    }

    @Operation(summary = "Seat map of a show")
    @GetMapping("/shows/{showId}/seats")
    public List<SeatResponse> seats(@PathVariable Long showId) {
        return browseService.seatMap(showId);
    }

    /** Price preview, e.g. /shows/5/quote?seats=A1,A2,A3 */
    @Operation(summary = "Price preview including offers (nothing is reserved)")
    @GetMapping("/shows/{showId}/quote")
    public PriceBreakdown quote(@PathVariable Long showId, @RequestParam("seats") List<String> seats) {
        return browseService.quote(showId, seats);
    }
}
