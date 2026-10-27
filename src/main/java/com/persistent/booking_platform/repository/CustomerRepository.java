package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
