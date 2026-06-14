package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.response.NotificationDto;
import com.shivam.bookMyShow.entity.Booking;
import java.util.List;
public interface NotificationService {
    List<NotificationDto> getMyNotifications();
    void sendBookingConfirmation(Booking booking);
    void sendBookingCancellation(Booking booking);
    void sendWaitlistNotification(Long userId, Long showId);
    void sendShowReminders();
}
