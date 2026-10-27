package com.persistent.booking_platform.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Always-successful stand-in for a payment provider (real integration is out of scope). */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public String createOrder(Long bookingId, BigDecimal amount) {
        return "ORD-" + bookingId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String charge(Long customerId, BigDecimal amount) {
        return "PAY-" + UUID.randomUUID();
    }

    @Override
    public String refund(Long customerId, BigDecimal amount) {
        return "REF-" + UUID.randomUUID();
    }
}
