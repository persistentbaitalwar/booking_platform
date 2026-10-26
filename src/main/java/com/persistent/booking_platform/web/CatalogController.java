package com.persistent.booking_platform.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import static com.persistent.booking_platform.dto.Requests.*;
import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Platform administration and customer registration. */
@Tag(name = "Catalog")
@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "Create a city")
    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse createCity(@Valid @RequestBody CityRequest request) {
        return catalogService.createCity(request);
    }

    @Operation(summary = "List cities")
    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return catalogService.listCities();
    }

    @Operation(summary = "Create a movie")
    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return catalogService.createMovie(request);
    }

    @Operation(summary = "List all movies")
    @GetMapping("/movies")
    public List<MovieResponse> listMovies() {
        return catalogService.listMovies();
    }

    @Operation(summary = "Add or replace the title of a movie in one language")
    @PostMapping("/movies/{movieId}/translations")
    public TranslationResponse addTranslation(@PathVariable Long movieId,
                                              @Valid @RequestBody TranslationRequest request) {
        return catalogService.addTranslation(movieId, request);
    }

    @Operation(summary = "Register a customer")
    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request) {
        return catalogService.createCustomer(request);
    }

    @Operation(summary = "Switch an offer on for a city, a theatre or everywhere")
    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse createOffer(@Valid @RequestBody OfferRequest request) {
        return catalogService.createOffer(request);
    }
}
