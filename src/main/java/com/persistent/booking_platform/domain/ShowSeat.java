package com.persistent.booking_platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Seat inventory of one show. This row is the unit of concurrency control: it is locked
 * pessimistically while booking and carries an optimistic version as a second safety net.
 */
@Entity
@Table(name = "show_seat",
        uniqueConstraints = @UniqueConstraint(name = "uk_show_seat", columnNames = {"show_id", "seat_number"}),
        indexes = {@Index(name = "idx_show_seat_booking", columnList = "booking_id"),
                @Index(name = "idx_show_seat_status", columnList = "show_id, status")})
public class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "show_id")
    private Show show;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Version
    private Long version;

    protected ShowSeat() {
    }

    public ShowSeat(Show show, String seatNumber, BigDecimal price, SeatStatus status) {
        this.show = show;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
    }

    public void book(Booking newBooking) {
        this.status = SeatStatus.BOOKED;
        this.booking = newBooking;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.booking = null;
    }

    public Long getId() { return id; }
    public Show getShow() { return show; }
    public String getSeatNumber() { return seatNumber; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    public Booking getBooking() { return booking; }
}
