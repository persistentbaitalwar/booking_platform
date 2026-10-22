package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.entity.Booking;
import com.persistent.booking_platform.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Prevents two concurrent cancellations or reschedules of the same booking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findForUpdate(@Param("id") Long id);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByShowIdAndStatusOrderById(Long showId, BookingStatus status);
}
