package com.persistent.booking_platform.pricing;

import java.math.BigDecimal;

public record TicketPrice(String seatNumber, BigDecimal price, BigDecimal discount, BigDecimal payable) {
}
