package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.OutboxEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Oldest unpublished events first; locked so two relay instances never publish the same rows concurrently. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.id")
    List<OutboxEvent> lockPending(Pageable page);

    List<OutboxEvent> findByAggregateIdOrderById(String aggregateId);
}
