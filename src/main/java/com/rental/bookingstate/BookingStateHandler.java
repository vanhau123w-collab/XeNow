package com.rental.bookingstate;

import com.rental.entity.Booking;

public interface BookingStateHandler {
    boolean supports(Booking.Status status);

    void apply(Booking booking, BookingStateContext context);
}
