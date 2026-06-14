package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Seat;
import com.shivam.bookMyShow.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreen(Screen screen);
    int countByScreen(Screen screen);
}
