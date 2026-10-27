package com.persistent.booking_platform.pricing;

import com.persistent.booking_platform.domain.OfferType;
import com.persistent.booking_platform.domain.Show;
import java.math.BigDecimal;

/**
 * Strategy pattern: one implementation per offer. Adding an offer means adding a class and an {@link OfferType}.
 */
public interface DiscountStrategy {

    OfferType supports();

    /**
     * @param ticketIndex zero-based position of the ticket inside the booking
     * @return discount percentage between 0 and 100 for that ticket
     */
    BigDecimal discountPercent(int ticketIndex, Show show);
}
