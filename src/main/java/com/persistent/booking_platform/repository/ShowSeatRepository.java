package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.SeatStatus;
import com.persistent.booking_platform.domain.ShowSeat;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * All locking queries order by id. Every transaction therefore acquires row locks in the same global
 * order, which removes the classic "A waits for B, B waits for A" deadlock between overlapping bookings.
 */
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.show.id = :showId and s.seatNumber in :seatNumbers order by s.id")
    List<ShowSeat> lockSeats(@Param("showId") Long showId, @Param("seatNumbers") Collection<String> seatNumbers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.id in :ids order by s.id")
    List<ShowSeat> lockByIds(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.show.id = :showId order by s.id")
    List<ShowSeat> lockAllForShow(@Param("showId") Long showId);

    /** Locks, in one statement, the seats currently held by a booking plus the requested seats of another show. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.booking.id = :bookingId "
            + "or (s.show.id = :newShowId and s.seatNumber in :seatNumbers) order by s.id")
    List<ShowSeat> lockForReschedule(@Param("bookingId") Long bookingId, @Param("newShowId") Long newShowId,
                                     @Param("seatNumbers") Collection<String> seatNumbers);

    List<ShowSeat> findByShowIdOrderById(Long showId);

    List<ShowSeat> findByShowIdAndSeatNumberIn(Long showId, Collection<String> seatNumbers);

    List<ShowSeat> findByBookingId(Long bookingId);

    @Query("select s.show.id, count(s) from ShowSeat s "
            + "where s.show.id in :showIds and s.status = :status group by s.show.id")
    List<Object[]> countByShowAndStatus(@Param("showIds") Collection<Long> showIds,
                                        @Param("status") SeatStatus status);

    long countByShowIdAndStatus(Long showId, SeatStatus status);
}
