package com.persistent.booking_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "theatre", uniqueConstraints = @UniqueConstraint(columnNames = {"city_id", "name"}))
public class Theatre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @ManyToOne(optional = false)
    @JoinColumn(name = "city_id")
    private City city;

    protected Theatre() {
    }

    public Theatre(String name, String address, City city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public City getCity() { return city; }
}
