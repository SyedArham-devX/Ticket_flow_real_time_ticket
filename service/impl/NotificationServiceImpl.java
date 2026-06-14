package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.response.NotificationDto;
import com.shivam.bookMyShow.entity.*;
import com.shivam.bookMyShow.entity.enums.*;
import com.shivam.bookMyShow.events.*;
import com.shivam.bookMyShow.exception.ResourceNotFoundException;
import com.shivam.bookMyShow.repository.*;
import com.shivam.bookMyShow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.shivam.bookMyShow.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final ModelMapper modelMapper;

    @Override
    public List<NotificationDto> getMyNotifications() {
        User user = getCurrentUser();
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(n -> modelMapper.map(n, NotificationDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @EventListener
    @Async
    public void sendBookingConfirmation(Booking booking) {
        String subject = "Booking Confirmed! 🎬 " + booking.getShow().getMovie().getTitle();
        String body = "Hi " + booking.getUser().getName() + ",\n\n"
                + "Your booking is CONFIRMED!\n\n"
                + "Movie: " + booking.getShow().getMovie().getTitle() + "\n"
                + "Date: " + booking.getShow().getShowDate() + "\n"
                + "Time: " + booking.getShow().getStartTime() + "\n"
                + "Seats: " + booking.getShowSeats().size() + " seat(s)\n"
                + "Total: ₹" + booking.getTotalAmount() + "\n\n"
                + "Booking ID: " + booking.getId() + "\n\n"
                + "Enjoy the show! 🍿";

        saveAndSendEmail(booking.getUser(), NotificationType.BOOKING_CONFIRMED, subject, body);
    }

    @Override
    @EventListener
    @Async
    public void sendBookingCancellation(Booking booking) {
        String subject = "Booking Cancelled - " + booking.getShow().getMovie().getTitle();
        String body = "Hi " + booking.getUser().getName() + ",\n\n"
                + "Your booking #" + booking.getId() + " has been cancelled.\n"
                + "Refund of ₹" + booking.getTotalAmount() + " will be processed in 5-7 business days.\n\n"
                + "We hope to see you soon!";

        saveAndSendEmail(booking.getUser(), NotificationType.BOOKING_CANCELLED, subject, body);
    }

    @Override
    @Async
    public void sendWaitlistNotification(Long userId, Long showId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        String subject = "Seats Available! 🎟️ " + show.getMovie().getTitle();
        String body = "Hi " + user.getName() + ",\n\n"
                + "Great news! Seats are now available for:\n"
                + "Movie: " + show.getMovie().getTitle() + "\n"
                + "Date: " + show.getShowDate() + "\n"
                + "Time: " + show.getStartTime() + "\n\n"
                + "Book now before they're gone! You have 15 minutes.\n\n"
                + "Hurry! 🏃";

        saveAndSendEmail(user, NotificationType.WAITLIST_AVAILABLE, subject, body);
    }

    @Scheduled(cron = "0 0 10 * * *")
    @Override
    public void sendShowReminders() {
        log.info("Running show reminder scheduler...");
        List<Show> todayShows = showRepository.findShowsForReminder(LocalDate.now());
        todayShows.forEach(show -> {
            log.info("Sending reminders for show: {}", show.getId());
        });
    }

    private void saveAndSendEmail(User user, NotificationType type, String subject, String body) {
        Notification notification = Notification.builder()
                .user(user).type(type).subject(subject).body(body)
                .status(NotificationStatus.PENDING).build();
        notification = notificationRepository.save(notification);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }
        notificationRepository.save(notification);
    }
}