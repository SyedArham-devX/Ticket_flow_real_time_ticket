package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Page<Movie> findByActiveTrue(Pageable pageable);

    @Query("SELECT DISTINCT m FROM Movie m JOIN Show s ON s.movie = m " +
           "JOIN s.screen sc JOIN sc.venue v JOIN v.city c " +
           "WHERE c.name = :cityName AND m.active = true")
    Page<Movie> findMoviesInCity(@Param("cityName") String cityName, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Movie m JOIN Show s ON s.movie = m " +
           "JOIN s.screen sc JOIN sc.venue v JOIN v.city c " +
           "WHERE c.name = :cityName AND m.genre = :genre AND m.active = true")
    Page<Movie> findMoviesInCityByGenre(@Param("cityName") String cityName,
                                         @Param("genre") String genre, Pageable pageable);
}
