package com.persistent.booking_platform.controllers;

import static com.persistent.booking_platform.model.Requests.*;
import static com.persistent.booking_platform.model.Responses.*;

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
@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse createCity(@Valid @RequestBody CityRequest request) {
        return catalogService.createCity(request);
    }

    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return catalogService.listCities();
    }

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return catalogService.createMovie(request);
    }

    @GetMapping("/movies")
    public List<MovieResponse> listMovies() {
        return catalogService.listMovies();
    }

    @PostMapping("/movies/{movieId}/translations")
    public TranslationResponse addTranslation(@PathVariable Long movieId,
                                              @Valid @RequestBody TranslationRequest request) {
        return catalogService.addTranslation(movieId, request);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request) {
        return catalogService.createCustomer(request);
    }

    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse createOffer(@Valid @RequestBody OfferRequest request) {
        return catalogService.createOffer(request);
    }
}
