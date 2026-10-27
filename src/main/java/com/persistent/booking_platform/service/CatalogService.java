package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.dto.Requests.*;
import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.domain.City;
import com.persistent.booking_platform.domain.Customer;
import com.persistent.booking_platform.domain.Movie;
import com.persistent.booking_platform.domain.MovieTranslation;
import com.persistent.booking_platform.domain.Offer;
import com.persistent.booking_platform.domain.Theatre;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.repository.CityRepository;
import com.persistent.booking_platform.repository.CustomerRepository;
import com.persistent.booking_platform.repository.MovieRepository;
import com.persistent.booking_platform.repository.MovieTranslationRepository;
import com.persistent.booking_platform.repository.OfferRepository;
import com.persistent.booking_platform.repository.TheatreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reference data: cities, movies, customers and offers. */
@Service
public class CatalogService {
    private final CityRepository cityRepository;
    private final MovieRepository movieRepository;
    private final CustomerRepository customerRepository;
    private final OfferRepository offerRepository;
    private final TheatreRepository theatreRepository;
    private final MovieTranslationRepository translationRepository;

    public CatalogService(CityRepository cityRepository, MovieRepository movieRepository,
                          CustomerRepository customerRepository, OfferRepository offerRepository,
                          TheatreRepository theatreRepository, MovieTranslationRepository translationRepository) {
        this.translationRepository = translationRepository;
        this.cityRepository = cityRepository;
        this.movieRepository = movieRepository;
        this.customerRepository = customerRepository;
        this.offerRepository = offerRepository;
        this.theatreRepository = theatreRepository;
    }

    @Transactional
    public CityResponse createCity(CityRequest request) {
        String name = request.name().trim();
        if (cityRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("CITY_EXISTS", "City '" + name + "' already exists");
        }
        City city = cityRepository.save(new City(name));
        return new CityResponse(city.getId(), city.getName());
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listCities() {
        return cityRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CityResponse(c.getId(), c.getName())).toList();
    }

    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = movieRepository.save(new Movie(request.title().trim(), request.language().trim(),
                request.genre().trim(), request.durationMinutes()));
        return toResponse(movie);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> listMovies() {
        return movieRepository.findAllByOrderByTitleAsc().stream().map(CatalogService::toResponse).toList();
    }

    /** Adds or replaces the title of a movie for one language (localisation of the catalogue). */
    @Transactional
    public TranslationResponse addTranslation(Long movieId, TranslationRequest request) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() ->
                ApiException.notFound("MOVIE_NOT_FOUND", "Movie " + movieId + " was not found"));
        String title = request.title().trim();
        MovieTranslation translation = translationRepository.findByMovieIdAndLocale(movieId, request.locale())
                .orElseGet(() -> new MovieTranslation(movie, request.locale(), title));
        translation.setTitle(title);
        translationRepository.save(translation);
        return new TranslationResponse(movieId, request.locale(), title);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        String email = request.email().trim();
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("CUSTOMER_EXISTS", "A customer with email '" + email + "' already exists");
        }
        Customer customer = customerRepository.save(new Customer(request.name().trim(), email));
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail());
    }

    @Transactional
    public OfferResponse createOffer(OfferRequest request) {
        String code = request.code().trim();
        if (offerRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("OFFER_EXISTS", "Offer code '" + code + "' already exists");
        }
        City city = request.cityId() == null ? null : cityRepository.findById(request.cityId())
                .orElseThrow(() -> ApiException.notFound("CITY_NOT_FOUND", "City " + request.cityId() + " was not found"));
        Theatre theatre = request.theatreId() == null ? null : theatreRepository.findById(request.theatreId())
                .orElseThrow(() -> ApiException.notFound("THEATRE_NOT_FOUND", "Theatre " + request.theatreId() + " was not found"));
        Offer offer = offerRepository.save(new Offer(code, request.type(), city, theatre));
        return new OfferResponse(offer.getId(), offer.getCode(), offer.getType(), offer.getType().getDescription(),
                city == null ? null : city.getId(), theatre == null ? null : theatre.getId());
    }

    private static MovieResponse toResponse(Movie m) {
        return new MovieResponse(m.getId(), m.getTitle(), m.getLanguage(), m.getGenre(), m.getDurationMinutes());
    }
}
