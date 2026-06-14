package com.shivam.bookMyShow.events;
import com.shivam.bookMyShow.entity.Booking;
import lombok.*;
@Getter @AllArgsConstructor
public class BookingConfirmedEvent {
    private final Booking booking;
}
