package com.persistent.booking_platform.pricing;

import com.persistent.booking_platform.domain.OfferType;
import com.persistent.booking_platform.domain.Show;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ThirdTicketDiscountStrategy implements DiscountStrategy {
    public static final BigDecimal PERCENT = new BigDecimal("50");
    private static final int THIRD_TICKET_INDEX = 2;

    @Override
    public OfferType supports() {
        return OfferType.THIRD_TICKET_DISCOUNT;
    }

    @Override
    public BigDecimal discountPercent(int ticketIndex, Show show) {
        return ticketIndex == THIRD_TICKET_INDEX ? PERCENT : BigDecimal.ZERO;
    }
}
