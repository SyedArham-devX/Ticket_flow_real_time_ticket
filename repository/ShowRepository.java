package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Movie;
import com.shivam.bookMyShow.entity.Show;
import com.shivam.bookMyShow.entity.Screen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieAndShowDate(Movie movie, LocalDate date);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.venue v JOIN v.city c " +
           "WHERE c.name = :cityName AND s.showDate = :date AND s.status = 'SCHEDULED'")
    Page<Show> findShowsByCityAndDate(@Param("cityName") String cityName,
                                      @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.venue v JOIN v.city c " +
           "WHERE c.name = :cityName AND s.movie.id = :movieId " +
           "AND s.showDate = :date AND s.status = 'SCHEDULED'")
    List<Show> findShowsByCityMovieAndDate(@Param("cityName") String cityName,
                                           @Param("movieId") Long movieId,
                                           @Param("date") LocalDate date);

    @Query("SELECT s FROM Show s WHERE s.showDate = :date AND s.status = 'SCHEDULED'")
    List<Show> findShowsForReminder(@Param("date") LocalDate date);
}
