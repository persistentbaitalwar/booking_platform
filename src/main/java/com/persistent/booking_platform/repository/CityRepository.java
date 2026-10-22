package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.entity.City;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
    boolean existsByNameIgnoreCase(String name);

    List<City> findAllByOrderByNameAsc();
}
