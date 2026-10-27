package com.persistent.booking_platform.pricing;

import com.persistent.booking_platform.domain.Offer;
import com.persistent.booking_platform.domain.OfferType;
import com.persistent.booking_platform.domain.Show;
import com.persistent.booking_platform.domain.ShowSeat;
import com.persistent.booking_platform.domain.Theatre;
import com.persistent.booking_platform.repository.OfferRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Prices a set of seats. For each ticket the single best applicable discount is used
 * (discounts are not stacked), so the third ticket of an afternoon show gets 50%, not 70%.
 */
@Service
public class PricingService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OfferRepository offerRepository;
    private final Map<OfferType, DiscountStrategy> strategies = new EnumMap<>(OfferType.class);

    public PricingService(OfferRepository offerRepository, List<DiscountStrategy> discountStrategies) {
        this.offerRepository = offerRepository;
        discountStrategies.forEach(strategy -> strategies.put(strategy.supports(), strategy));
    }

    public List<Offer> applicableOffers(Theatre theatre) {
        return offerRepository.findByEnabledTrue().stream()
                .filter(offer -> offer.appliesTo(theatre))
                .toList();
    }

    /** @param seats seats in the order the customer selected them (the third one is the "third ticket") */
    public PriceBreakdown price(Show show, List<ShowSeat> seats) {
        Set<OfferType> activeTypes = EnumSet.noneOf(OfferType.class);
        applicableOffers(show.getTheatre()).forEach(offer -> activeTypes.add(offer.getType()));
        List<DiscountStrategy> active = activeTypes.stream().map(strategies::get).filter(s -> s != null).toList();

        List<TicketPrice> tickets = new ArrayList<>();
        Set<OfferType> used = EnumSet.noneOf(OfferType.class);
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        for (int i = 0; i < seats.size(); i++) {
            ShowSeat seat = seats.get(i);
            BigDecimal bestPercent = BigDecimal.ZERO;
            DiscountStrategy bestStrategy = null;
            for (DiscountStrategy strategy : active) {
                BigDecimal percent = strategy.discountPercent(i, show);
                if (percent.compareTo(bestPercent) > 0) {
                    bestPercent = percent;
                    bestStrategy = strategy;
                }
            }
            BigDecimal ticketDiscount = seat.getPrice().multiply(bestPercent)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            if (bestStrategy != null) {
                used.add(bestStrategy.supports());
            }
            tickets.add(new TicketPrice(seat.getSeatNumber(), seat.getPrice(), ticketDiscount,
                    seat.getPrice().subtract(ticketDiscount)));
            gross = gross.add(seat.getPrice());
            discount = discount.add(ticketDiscount);
        }
        return new PriceBreakdown(List.copyOf(tickets), gross, discount, gross.subtract(discount), List.copyOf(used));
    }
}
