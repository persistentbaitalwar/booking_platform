package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.Offer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    boolean existsByCodeIgnoreCase(String code);

    List<Offer> findByEnabledTrue();
}
