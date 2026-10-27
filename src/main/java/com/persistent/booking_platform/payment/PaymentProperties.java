package com.persistent.booking_platform.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * mode = hold: a booking waits (seats held) until the gateway confirms the payment; mode = immediate: the payment is
 * taken inside the booking transaction (only sensible with the simulated gateway and in tests).
 */
@Component
public class PaymentProperties {
    private final String mode;
    private final long holdMinutes;
    private final String webhookSecret;
    private final boolean simulatorEnabled;

    public PaymentProperties(@Value("${app.payments.mode}") String mode,
                             @Value("${app.payments.hold-minutes}") long holdMinutes,
                             @Value("${app.payments.webhook-secret}") String webhookSecret,
                             @Value("${app.payments.simulator-enabled}") boolean simulatorEnabled) {
        if (webhookSecret == null || webhookSecret.length() < 16) {
            throw new IllegalStateException("app.payments.webhook-secret must be at least 16 characters long");
        }
        this.mode = mode;
        this.holdMinutes = holdMinutes;
        this.webhookSecret = webhookSecret;
        this.simulatorEnabled = simulatorEnabled;
    }

    public boolean isHoldMode() { return "hold".equalsIgnoreCase(mode); }
    public long holdMinutes() { return holdMinutes; }
    public String webhookSecret() { return webhookSecret; }
    public boolean simulatorEnabled() { return simulatorEnabled; }
}
