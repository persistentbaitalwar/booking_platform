package com.persistent.booking_platform.security;

import static com.persistent.booking_platform.dto.Responses.AuthResponse;

import com.persistent.booking_platform.domain.UserAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration lifetime;

    public JwtService(JwtEncoder encoder, Clock clock, @Value("${app.security.token-minutes}") long minutes) {
        this.encoder = encoder;
        this.clock = clock;
        this.lifetime = Duration.ofMinutes(minutes);
    }

    public AuthResponse issue(UserAccount user) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("movie-booking")
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name());
        if (user.getCustomerId() != null) {
            claims.claim("customerId", user.getCustomerId());
        }
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
        return new AuthResponse(token, "Bearer", lifetime.toSeconds(), user.getRole().name(), user.getId(),
                user.getCustomerId(), user.getDisplayName());
    }
}
