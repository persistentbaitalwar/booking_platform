package com.persistent.booking_platform.entity;

import jakarta.persistence.*;

/**
 * Switches an {@link OfferType} on for a scope: one theatre, one city, or (when both are empty) everywhere.
 */
@Entity
@Table(name = "offer")
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OfferType type;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne
    @JoinColumn(name = "theatre_id")
    private Theatre theatre;

    protected Offer() {
    }

    public Offer(String code, OfferType type, City city, Theatre theatre) {
        this.code = code;
        this.type = type;
        this.city = city;
        this.theatre = theatre;
    }

    /** A theatre-level offer wins over a city-level one; an offer with no scope applies everywhere. */
    public boolean appliesTo(Theatre target) {
        if (!enabled) {
            return false;
        }
        if (theatre != null) {
            return theatre.getId().equals(target.getId());
        }
        if (city != null) {
            return city.getId().equals(target.getCity().getId());
        }
        return true;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public OfferType getType() { return type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public City getCity() { return city; }
    public Theatre getTheatre() { return theatre; }
}
