package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByCustomerIdOrderByIdDesc(Long customerId);

    List<Notification> findByBookingIdOrderById(Long bookingId);
}
