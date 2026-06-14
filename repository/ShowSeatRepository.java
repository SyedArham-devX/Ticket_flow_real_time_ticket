package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Booking;
import com.shivam.bookMyShow.entity.Show;
import com.shivam.bookMyShow.entity.ShowSeat;
import com.shivam.bookMyShow.entity.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    List<ShowSeat> findByShow(Show show);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.id IN :seatIds")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ShowSeat> findAndLockSeats(@Param("showId") Long showId,
                                    @Param("seatIds") List<Long> seatIds);

    @Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.status = 'AVAILABLE'")
    long countAvailableSeats(@Param("showId") Long showId);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = 'LOCKED' AND ss.lockedAt < :expiry")
    List<ShowSeat> findExpiredLockedSeats(@Param("expiry") LocalDateTime expiry);

    List<ShowSeat> findByBooking(Booking booking);

    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.status = 'AVAILABLE', ss.lockedAt = null, ss.booking = null " +
           "WHERE ss.status = 'LOCKED' AND ss.lockedAt < :expiry")
    void releaseExpiredLocks(@Param("expiry") LocalDateTime expiry);
}
