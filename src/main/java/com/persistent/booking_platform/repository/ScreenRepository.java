package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.Screen;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    boolean existsByTheatreIdAndNameIgnoreCase(Long theatreId, String name);

    /** Serialises show scheduling on one screen so that two shows can never overlap. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Screen s where s.id = :id")
    Optional<Screen> findForUpdate(@Param("id") Long id);
}
