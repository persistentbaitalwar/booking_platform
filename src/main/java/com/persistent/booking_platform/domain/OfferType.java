package com.persistent.booking_platform.domain;

public enum OfferType {
    THIRD_TICKET_DISCOUNT("50% discount on the third ticket"),
    AFTERNOON_SHOW_DISCOUNT("20% discount on afternoon shows");

    private final String description;

    OfferType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
