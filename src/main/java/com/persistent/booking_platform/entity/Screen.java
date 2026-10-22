package com.persistent.booking_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "screen", uniqueConstraints = @UniqueConstraint(columnNames = {"theatre_id", "name"}))
public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "theatre_id")
    private Theatre theatre;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "seat_rows", nullable = false)
    private int rows;

    @Column(nullable = false)
    private int seatsPerRow;

    protected Screen() {
    }

    public Screen(Theatre theatre, String name, int rows, int seatsPerRow) {
        this.theatre = theatre;
        this.name = name;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
    }

    public Long getId() { return id; }
    public Theatre getTheatre() { return theatre; }
    public String getName() { return name; }
    public int getRows() { return rows; }
    public int getSeatsPerRow() { return seatsPerRow; }
}
