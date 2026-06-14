package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCase(String name);
}
