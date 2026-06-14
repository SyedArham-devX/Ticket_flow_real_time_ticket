package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Venue;
import com.shivam.bookMyShow.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByCityAndActiveTrue(City city);
}
