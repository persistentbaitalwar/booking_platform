package com.persistent.booking_platform.pricing;

import com.persistent.booking_platform.domain.OfferType;
import com.persistent.booking_platform.domain.Show;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class AfternoonShowDiscountStrategy implements DiscountStrategy {
    public static final BigDecimal PERCENT = new BigDecimal("20");
    /** Afternoon shows start from 12:00 (inclusive) until 17:00 (exclusive). */
    public static final LocalTime AFTERNOON_START = LocalTime.NOON;
    public static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);

    @Override
    public OfferType supports() {
        return OfferType.AFTERNOON_SHOW_DISCOUNT;
    }

    @Override
    public BigDecimal discountPercent(int ticketIndex, Show show) {
        LocalTime start = show.getStartTime();
        boolean afternoon = !start.isBefore(AFTERNOON_START) && start.isBefore(AFTERNOON_END);
        return afternoon ? PERCENT : BigDecimal.ZERO;
    }
}
