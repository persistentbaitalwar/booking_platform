package com.persistent.booking_platform.domain;

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

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    protected Theatre() {
    }

    public Theatre(String name, String address, City city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public void assignOwner(Long userId) { this.ownerUserId = userId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public City getCity() { return city; }
}
