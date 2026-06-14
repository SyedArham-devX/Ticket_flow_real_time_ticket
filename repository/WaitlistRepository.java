package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Show;
import com.shivam.bookMyShow.entity.User;
import com.shivam.bookMyShow.entity.WaitlistEntry;
import com.shivam.bookMyShow.entity.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    Optional<WaitlistEntry> findByUserAndShow(User user, Show show);
    List<WaitlistEntry> findByShowAndStatusOrderByPositionAsc(Show show, WaitlistStatus status);

    @Query("SELECT COUNT(w) FROM WaitlistEntry w WHERE w.show = :show AND w.status = 'WAITING'")
    int countWaitingByShow(@Param("show") Show show);

    @Query("SELECT MAX(w.position) FROM WaitlistEntry w WHERE w.show = :show")
    Optional<Integer> findMaxPositionByShow(@Param("show") Show show);
}
