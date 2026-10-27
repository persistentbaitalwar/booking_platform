package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.Booking;
import com.persistent.booking_platform.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    List<Booking> findByShowIdAndStatusInOrderById(Long showId, Collection<BookingStatus> statuses);

    @Query("select b.id from Booking b where b.status = :status and b.holdExpiresAt < :now order by b.id")
    List<Long> findExpiredHoldIds(@Param("status") BookingStatus status, @Param("now") Instant now, Pageable page);
}
