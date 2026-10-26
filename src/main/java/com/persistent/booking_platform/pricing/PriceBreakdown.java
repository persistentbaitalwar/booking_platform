package com.persistent.booking_platform.pricing;

import com.persistent.booking_platform.domain.OfferType;
import java.math.BigDecimal;
import java.util.List;

public record PriceBreakdown(List<TicketPrice> tickets, BigDecimal gross, BigDecimal discount, BigDecimal net,
                             List<OfferType> appliedOffers) {
}
