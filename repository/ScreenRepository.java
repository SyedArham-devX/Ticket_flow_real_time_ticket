package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Screen;
import com.shivam.bookMyShow.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByVenue(Venue venue);
}
