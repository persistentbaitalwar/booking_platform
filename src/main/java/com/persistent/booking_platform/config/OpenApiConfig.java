package com.persistent.booking_platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI: /swagger-ui.html, raw document: /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI movieBookingOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Sign in with POST /api/v1/auth/login, then paste the token here")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(new Info()
                        .title("Movie Ticket Booking API")
                        .version("1.0.0")
                        .description("B2C APIs for customers (browse, book, cancel, reschedule, QR tickets) and "
                                + "B2B APIs for theatre partners (onboarding, shows, seat inventory, bulk import). "
                                + "Errors use one format: timestamp, status, code, message and details."))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register and sign in (public)"),
                        new Tag().name("Payments").description("Payment provider webhook and the local demo gateway"),
                        new Tag().name("Browse").description("Movies, theatres, show times, seat maps and price quotes"),
                        new Tag().name("Bookings").description("Book, cancel, reschedule, bulk operations and QR codes"),
                        new Tag().name("Notifications").description("Messages produced by the Kafka ticket event pipeline"),
                        new Tag().name("Partner").description("Theatre partner APIs: theatres, screens, shows, seat inventory"),
                        new Tag().name("Catalog").description("Cities, movies, translations, customers and offers")));
    }
}
