package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.request.InitiateBookingRequest;
import com.shivam.bookMyShow.dto.response.BookingDto;
import com.shivam.bookMyShow.entity.enums.BookingStatus;
import com.stripe.model.Event;
import java.util.List;
public interface BookingService {
    BookingDto initiateBooking(InitiateBookingRequest request);
    String initiatePayment(Long bookingId);
    BookingStatus getBookingStatus(Long bookingId);
    void cancelBooking(Long bookingId);
    List<BookingDto> getMyBookings();
    BookingDto getBookingById(Long bookingId);
    void capturePayment(Event event);
}
