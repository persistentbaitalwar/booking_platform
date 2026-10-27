package com.persistent.booking_platform.security;

import com.persistent.booking_platform.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Temporary lock-out after repeated wrong passwords (OWASP A07). Kept in memory per instance; use Redis to share
 * the counters across instances in production.
 */
@Service
public class LoginAttemptService {
    private record State(int failures, Instant lockedUntil) {
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginAttemptService(Clock clock, @Value("${app.security.max-failed-logins}") int maxFailures,
                               @Value("${app.security.lockout-minutes}") long lockoutMinutes) {
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockoutMinutes);
    }

    public void assertNotLocked(String email) {
        State state = states.get(key(email));
        if (state != null && state.lockedUntil() != null && state.lockedUntil().isAfter(clock.instant())) {
            long minutes = Math.max(1, Duration.between(clock.instant(), state.lockedUntil()).toMinutes() + 1);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT_LOCKED",
                    "Too many failed sign-in attempts. Please try again in " + minutes + " minute(s).");
        }
    }

    public void recordFailure(String email) {
        states.merge(key(email), new State(1, null), (old, ignored) -> {
            int failures = (old.lockedUntil() != null && old.lockedUntil().isBefore(clock.instant()) ? 0 : old.failures()) + 1;
            return failures >= maxFailures ? new State(0, clock.instant().plus(lockDuration)) : new State(failures, null);
        });
    }

    public void recordSuccess(String email) {
        states.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
