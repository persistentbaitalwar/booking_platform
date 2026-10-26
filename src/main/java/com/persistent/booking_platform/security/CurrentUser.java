package com.persistent.booking_platform.security;

import com.persistent.booking_platform.domain.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/** The signed-in user, read from the claims of the access token. */
public record CurrentUser(Long userId, Role role, Long customerId) {

    public static CurrentUser from(Jwt jwt) {
        Object customer = jwt.getClaim("customerId");
        return new CurrentUser(Long.valueOf(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString("role")),
                customer instanceof Number n ? n.longValue() : null);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
