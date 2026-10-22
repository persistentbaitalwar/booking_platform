package com.persistent.booking_platform.repository;

import com.persistent.booking_platform.entity.Movie;
import com.persistent.booking_platform.entity.Show;
import com.persistent.booking_platform.entity.ShowStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("select s from Show s "
            + "where s.movie.id = :movieId and s.screen.theatre.city.id = :cityId "
            + "and s.showDate = :date and s.status = :status "
            + "order by s.screen.theatre.name, s.startTime")
    List<Show> findRunningShows(@Param("movieId") Long movieId, @Param("cityId") Long cityId,
                                @Param("date") LocalDate date, @Param("status") ShowStatus status);

    @Query("select distinct s.movie from Show s where s.screen.theatre.city.id = :cityId "
            + "and s.status = :status and s.showDate >= :fromDate")
    List<Movie> findMoviesInCity(@Param("cityId") Long cityId, @Param("fromDate") LocalDate fromDate,
                                 @Param("status") ShowStatus status);

    List<Show> findByScreenIdAndShowDateInAndStatus(Long screenId, Collection<LocalDate> dates, ShowStatus status);
}
