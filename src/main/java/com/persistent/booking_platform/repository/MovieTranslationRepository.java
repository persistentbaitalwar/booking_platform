package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.entity.MovieTranslation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieTranslationRepository extends JpaRepository<MovieTranslation, Long> {
    Optional<MovieTranslation> findByMovieIdAndLocale(Long movieId, String locale);

    List<MovieTranslation> findByMovieIdInAndLocale(Collection<Long> movieIds, String locale);
}
