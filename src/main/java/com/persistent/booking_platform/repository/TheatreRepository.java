package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    boolean existsByCityIdAndNameIgnoreCase(Long cityId, String name);
}
