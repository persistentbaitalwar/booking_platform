package com.persistent.booking_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "city")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected City() {
    }

    public City(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
