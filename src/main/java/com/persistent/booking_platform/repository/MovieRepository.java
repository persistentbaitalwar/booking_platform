package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.domain.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findAllByOrderByTitleAsc();
}
