package com.shivam.bookMyShow.repository;
import com.shivam.bookMyShow.entity.Notification;
import com.shivam.bookMyShow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}
