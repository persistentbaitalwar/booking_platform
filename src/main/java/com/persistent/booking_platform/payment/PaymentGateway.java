package com.persistent.booking_platform.payment;

import java.math.BigDecimal;

/** Port to a real payment provider. Called inside the booking transaction, so a failure rolls the booking back. */
public interface PaymentGateway {

    /** Creates a payment order at the provider for a booking that waits for payment; returns the order reference. */
    String createOrder(Long bookingId, BigDecimal amount);

    String charge(Long customerId, BigDecimal amount);

    String refund(Long customerId, BigDecimal amount);
}
