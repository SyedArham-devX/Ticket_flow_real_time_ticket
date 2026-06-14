package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.request.InitiateBookingRequest;
import com.shivam.bookMyShow.dto.response.*;
import com.shivam.bookMyShow.entity.*;
import com.shivam.bookMyShow.entity.enums.*;
import com.shivam.bookMyShow.events.*;
import com.shivam.bookMyShow.exception.*;
import com.shivam.bookMyShow.repository.*;
import com.shivam.bookMyShow.service.BookingService;
import com.shivam.bookMyShow.service.WaitlistService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.shivam.bookMyShow.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final WaitlistService waitlistService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public BookingDto initiateBooking(InitiateBookingRequest request) {
        log.info("Initiating booking for show: {}, seats: {}", request.getShowId(), request.getShowSeatIds());

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.getShowId()));

        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new IllegalStateException("Show is not available for booking");
        }

        List<ShowSeat> seats = showSeatRepository.findAndLockSeats(request.getShowId(), request.getShowSeatIds());

        if (seats.size() != request.getShowSeatIds().size()) {
            throw new ResourceNotFoundException("Some seats not found for this show");
        }

        seats.forEach(seat -> {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Seat " + seat.getSeat().getRowNumber()
                        + seat.getSeat().getSeatNumber() + " is not available");
            }
        });

        User user = getCurrentUser();

        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .status(BookingStatus.INITIATED)
                .totalAmount(seats.stream().map(ShowSeat::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
        booking = bookingRepository.save(booking);

        final Booking savedBooking = booking;
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedAt(LocalDateTime.now());
            seat.setBooking(savedBooking);
        });
        showSeatRepository.saveAll(seats);

        log.info("Booking initiated successfully: {}", booking.getId());
        return toBookingDto(booking, seats);
    }

    @Override
    @Transactional
    public String initiatePayment(Long bookingId) {
        Booking booking = getBookingAndValidateOwner(bookingId);

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has expired. Please try again.");
        }

        try {
            List<ShowSeat> seats = showSeatRepository.findByBooking(booking);
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(booking.getUser().getEmail())
                    .setSuccessUrl(frontendUrl + "/bookings/" + bookingId + "/status")
                    .setCancelUrl(frontendUrl + "/bookings/" + bookingId + "/status")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("inr")
                                    .setUnitAmount(booking.getTotalAmount()
                                            .multiply(BigDecimal.valueOf(100)).longValue())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(booking.getShow().getMovie().getTitle())
                                            .setDescription("Booking ID: " + booking.getId()
                                                    + " | " + seats.size() + " seat(s)")
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            booking.setPaymentSessionId(session.getId());
            booking.setStatus(BookingStatus.PAYMENT_PENDING);
            bookingRepository.save(booking);
            return session.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    public BookingStatus getBookingStatus(Long bookingId) {
        return getBookingAndValidateOwner(bookingId).getStatus();
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = getBookingAndValidateOwner(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<ShowSeat> seats = showSeatRepository.findByBooking(booking);
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setBooking(null);
        });
        showSeatRepository.saveAll(seats);

        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent()).build());
        } catch (StripeException e) {
            log.error("Refund failed for booking: {}", bookingId, e);
        }

        waitlistService.notifyWaitlist(booking.getShow().getId());
        eventPublisher.publishEvent(new BookingCancelledEvent(booking));
        log.info("Booking cancelled: {}", bookingId);
    }

    @Override
    public List<BookingDto> getMyBookings() {
        User user = getCurrentUser();
        return bookingRepository.findByUser(user).stream()
                .map(b -> toBookingDto(b, showSeatRepository.findByBooking(b)))
                .collect(Collectors.toList());
    }

    @Override
    public BookingDto getBookingById(Long bookingId) {
        Booking booking = getBookingAndValidateOwner(bookingId);
        return toBookingDto(booking, showSeatRepository.findByBooking(booking));
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) return;

            Booking booking = bookingRepository.findByPaymentSessionId(session.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found for session: " + session.getId()));

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            List<ShowSeat> seats = showSeatRepository.findByBooking(booking);
            seats.forEach(seat -> seat.setStatus(SeatStatus.BOOKED));
            showSeatRepository.saveAll(seats);

            eventPublisher.publishEvent(new BookingConfirmedEvent(booking));
            log.info("Payment captured and booking confirmed: {}", booking.getId());
        }
    }

    private Booking getBookingAndValidateOwner(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        User user = getCurrentUser();
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new AccessDeniedException("This booking does not belong to you");
        }
        return booking;
    }

    private boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    private BookingDto toBookingDto(Booking booking, List<ShowSeat> seats) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setStatus(booking.getStatus());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setCreatedAt(booking.getCreatedAt());

        if (booking.getShow() != null) {
            ShowDto showDto = new ShowDto();
            showDto.setId(booking.getShow().getId());
            showDto.setShowDate(booking.getShow().getShowDate());
            showDto.setStartTime(booking.getShow().getStartTime());
            showDto.setEndTime(booking.getShow().getEndTime());
            showDto.setLanguage(booking.getShow().getLanguage());
            showDto.setStatus(booking.getShow().getStatus());
            if (booking.getShow().getMovie() != null) {
                MovieDto movieDto = new MovieDto();
                movieDto.setId(booking.getShow().getMovie().getId());
                movieDto.setTitle(booking.getShow().getMovie().getTitle());
                showDto.setMovie(movieDto);
            }
            dto.setShow(showDto);
        }

        if (seats != null) {
            dto.setShowSeats(seats.stream().map(ss -> {
                ShowSeatDto seatDto = new ShowSeatDto();
                seatDto.setId(ss.getId());
                seatDto.setRowNumber(ss.getSeat().getRowNumber());
                seatDto.setSeatNumber(ss.getSeat().getSeatNumber());
                seatDto.setSeatType(ss.getSeat().getType());
                seatDto.setStatus(ss.getStatus());
                seatDto.setPrice(ss.getPrice());
                return seatDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}