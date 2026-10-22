package com.persistent.booking_platform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shows", indexes = @Index(name = "idx_show_screen_date", columnList = "screen_id, show_date"))
public class Show {
    /** Minutes needed between two shows on the same screen for cleaning and entry. */
    public static final int TURNAROUND_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "screen_id")
    private Screen screen;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowStatus status = ShowStatus.ACTIVE;

    protected Show() {
    }

    public Show(Movie movie, Screen screen, LocalDate showDate, LocalTime startTime, BigDecimal basePrice) {
        this.movie = movie;
        this.screen = screen;
        this.showDate = showDate;
        this.startTime = startTime;
        this.basePrice = basePrice;
    }

    public LocalDateTime startDateTime() {
        return LocalDateTime.of(showDate, startTime);
    }

    public LocalDateTime endDateTime() {
        return startDateTime().plusMinutes((long) movie.getDurationMinutes() + TURNAROUND_MINUTES);
    }

    public void reschedule(LocalDate newDate, LocalTime newStartTime) {
        this.showDate = newDate;
        this.startTime = newStartTime;
    }

    public void cancel() {
        this.status = ShowStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public Movie getMovie() { return movie; }
    public Screen getScreen() { return screen; }
    public Theatre getTheatre() { return screen.getTheatre(); }
    public LocalDate getShowDate() { return showDate; }
    public LocalTime getStartTime() { return startTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public ShowStatus getStatus() { return status; }
}
