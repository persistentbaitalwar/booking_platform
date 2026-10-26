package com.persistent.booking_platform.config;

import static com.persistent.booking_platform.dto.Requests.*;

import com.persistent.booking_platform.domain.OfferType;
import com.persistent.booking_platform.security.AuthService;
import com.persistent.booking_platform.service.CatalogService;
import com.persistent.booking_platform.service.PartnerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Loads a small demo data set on start-up (disable with app.seed-demo-data=false, always off in production).
 * Demo sign-ins: demo@example.com / Demo@12345 (customer), partner@example.com / Partner@12345 (owns Cinema Palace),
 * admin@example.com / Admin@123456789 (administrator).
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private final CatalogService catalogService;
    private final PartnerService partnerService;
    private final AuthService authService;

    public DemoDataSeeder(CatalogService catalogService, PartnerService partnerService, AuthService authService) {
        this.catalogService = catalogService;
        this.partnerService = partnerService;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        var city = catalogService.createCity(new CityRequest("Nagpur"));
        authService.registerCustomer(new RegisterRequest("Demo Customer", "demo@example.com", "Demo@12345"));
        var partner = authService.registerPartner(new RegisterRequest("Demo Partner", "partner@example.com", "Partner@12345"));
        authService.createAdmin("admin@example.com", "Admin@123456789");
        var theatre = partnerService.onboardTheatre(new TheatreRequest("Cinema Palace", "Civil Lines", city.id()),
                partner.userId());
        var screen = partnerService.addScreen(theatre.id(), new ScreenRequest("Screen 1", 5, 10));
        var movie = catalogService.createMovie(new MovieRequest("Space Odyssey", "English", "Sci-Fi", 120));
        var hindiMovie = catalogService.createMovie(new MovieRequest("Monsoon Melody", "Hindi", "Drama", 135));
        catalogService.addTranslation(hindiMovie.id(), new TranslationRequest("hi", "Monsoon Melody (Hindi title)"));
        catalogService.createOffer(new OfferRequest("THIRD50", OfferType.THIRD_TICKET_DISCOUNT, city.id(), null));
        catalogService.createOffer(new OfferRequest("NOON20", OfferType.AFTERNOON_SHOW_DISCOUNT, null, theatre.id()));
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        for (LocalTime time : new LocalTime[]{LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(19, 0)}) {
            partnerService.createShow(new ShowRequest(movie.id(), screen.id(), tomorrow, time, new BigDecimal("250.00")));
        }
        var screen2 = partnerService.addScreen(theatre.id(), new ScreenRequest("Screen 2", 4, 8));
        partnerService.createShow(new ShowRequest(hindiMovie.id(), screen2.id(), tomorrow, LocalTime.of(13, 0),
                new BigDecimal("180.00")));
        partnerService.createShow(new ShowRequest(hindiMovie.id(), screen2.id(), tomorrow, LocalTime.of(18, 30),
                new BigDecimal("180.00")));
    }
}
