package com.persistent.booking_platform.security;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT security. Browse endpoints are public; booking needs a CUSTOMER, the partner API needs a PARTNER,
 * platform administration needs an ADMIN. Ownership (my booking, my theatre) is checked in {@link AccessGuard}.
 * On AWS the same resource-server setup can validate Cognito tokens instead (set an issuer-uri and drop the encoder).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityHandlers handlers,
                                                   JwtAuthenticationConverter converter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // no cookies or sessions: tokens are sent in a header
                .cors(Customizer.withDefaults())
                .headers(AbstractHttpConfigurer::disable) // SecurityHeadersFilter sets the headers
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher("/api/v1/auth/**"), antMatcher("/api/v1/payments/webhook")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/v1/cities/**"),
                                antMatcher(HttpMethod.GET, "/api/v1/movies/**"),
                                antMatcher(HttpMethod.GET, "/api/v1/shows/**")).permitAll()
                        .requestMatchers(antMatcher("/v3/api-docs/**"), antMatcher("/swagger-ui/**"),
                                antMatcher("/swagger-ui.html"), antMatcher("/v3/api-docs.yaml"), antMatcher("/actuator/health/**"),
                                antMatcher("/actuator/health"), antMatcher("/h2-console/**")).permitAll()
                        .requestMatchers(antMatcher("/actuator/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/api/v1/partner/**")).hasAnyRole("PARTNER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/cities"),
                                antMatcher(HttpMethod.POST, "/api/v1/movies"),
                                antMatcher(HttpMethod.POST, "/api/v1/movies/*/translations"),
                                antMatcher(HttpMethod.POST, "/api/v1/customers"),
                                antMatcher(HttpMethod.POST, "/api/v1/offers")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/api/v1/**")).hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public SecretKey jwtSigningKey(@Value("${app.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.security.jwt-secret must be at least 32 characters long");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
