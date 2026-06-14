package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Booking;
import com.shivam.bookMyShow.entity.Show;
import com.shivam.bookMyShow.entity.User;
import com.shivam.bookMyShow.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    Optional<Booking> findByPaymentSessionId(String sessionId);
    List<Booking> findByShow(Show show);

    @Query("SELECT b FROM Booking b WHERE b.show.id = :showId AND b.status = :status")
    List<Booking> findByShowIdAndStatus(@Param("showId") Long showId,
                                        @Param("status") BookingStatus status);

    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal getTotalRevenue(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT b.show.movie.title, COUNT(b), SUM(b.totalAmount) FROM Booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt BETWEEN :start AND :end " +
           "GROUP BY b.show.movie.title ORDER BY SUM(b.totalAmount) DESC")
    List<Object[]> getTopMoviesByRevenue(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          org.springframework.data.domain.Pageable pageable);

    @Query("SELECT HOUR(b.createdAt), COUNT(b) FROM Booking b " +
           "WHERE b.status = 'CONFIRMED' GROUP BY HOUR(b.createdAt) ORDER BY HOUR(b.createdAt)")
    List<Object[]> getBookingsByHour();
}
