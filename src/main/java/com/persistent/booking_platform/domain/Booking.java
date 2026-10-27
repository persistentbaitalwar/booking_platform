package com.persistent.booking_platform.domain;

import com.persistent.booking_platform.pricing.PriceBreakdown;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "booking", indexes = {@Index(name = "idx_booking_customer", columnList = "customer_id"),
        @Index(name = "idx_booking_show", columnList = "show_id, status")})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "show_id")
    private Show show;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 500)
    private String seatNumbers;

    @Column(length = 200)
    private String appliedOffers = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant cancelledAt;

    /** Set while the booking waits for payment: the seats are held until this moment. */
    private Instant holdExpiresAt;

    @Column(length = 100)
    private String paymentReference;

    @Version
    private Long version;

    protected Booking() {
    }

    public Booking(Customer customer, Show show, PriceBreakdown price, List<String> seatNumbers) {
        this.customer = customer;
        this.show = show;
        applyPrice(price, seatNumbers);
    }

    /** Moves the booking to a new show/seat set and re-prices it. */
    public void moveTo(Show newShow, PriceBreakdown price, List<String> newSeatNumbers) {
        this.show = newShow;
        applyPrice(price, newSeatNumbers);
    }

    public void markPendingPayment(Instant holdUntil) {
        this.status = BookingStatus.PENDING_PAYMENT;
        this.holdExpiresAt = holdUntil;
    }

    public void confirm(String reference) {
        this.status = BookingStatus.CONFIRMED;
        this.holdExpiresAt = null;
        this.paymentReference = reference;
    }

    public void cancel(Instant when) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = when;
    }

    private void applyPrice(PriceBreakdown price, List<String> seats) {
        this.grossAmount = price.gross();
        this.discountAmount = price.discount();
        this.netAmount = price.net();
        this.seatNumbers = String.join(",", seats);
        this.appliedOffers = price.appliedOffers().stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public List<String> seatNumberList() {
        return Arrays.asList(seatNumbers.split(","));
    }

    public List<String> appliedOfferList() {
        return appliedOffers == null || appliedOffers.isEmpty() ? List.of() : Arrays.asList(appliedOffers.split(","));
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Show getShow() { return show; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
}
