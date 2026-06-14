package com.shivam.bookMyShow.events;
import com.shivam.bookMyShow.entity.Booking;
import lombok.*;
@Getter @AllArgsConstructor
public class BookingCancelledEvent {
    private final Booking booking;
}
