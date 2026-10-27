package com.persistent.booking_platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.persistent.booking_platform.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client request limit (OWASP A04): sign-in and registration calls and booking calls are counted per client
 * address and minute. Kept in memory per instance; at the edge AWS WAF rate rules do the same for the whole fleet.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final int authLimit;
    private final int bookingLimit;

    public RateLimitFilter(Clock clock, ObjectMapper objectMapper,
                           @Value("${app.ratelimit.auth-per-minute}") int authLimit,
                           @Value("${app.ratelimit.booking-per-minute}") int bookingLimit) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.authLimit = authLimit;
        this.bookingLimit = bookingLimit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String bucket = null;
        int limit = 0;
        if (path.startsWith("/api/v1/auth/")) {
            bucket = "auth";
            limit = authLimit;
        } else if ("POST".equals(request.getMethod()) && path.startsWith("/api/v1/bookings")) {
            bucket = "booking";
            limit = bookingLimit;
        }
        if (bucket != null && exceeded(bucket + "|" + request.getRemoteAddr(), limit)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiError.of(429, "RATE_LIMITED",
                    "Too many requests. Please wait a minute and try again.", List.of()));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean exceeded(String clientKey, int limit) {
        long minute = clock.millis() / 60_000;
        if (counters.size() > 10_000) {
            counters.keySet().removeIf(k -> !k.endsWith("|" + minute));
        }
        return counters.computeIfAbsent(clientKey + "|" + minute, k -> new AtomicInteger()).incrementAndGet() > limit;
    }
}
