package com.rental.bookingstate;

import com.rental.entity.Booking;
import com.rental.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class OngoingBookingStateHandler implements BookingStateHandler {

    @Override
    public boolean supports(Booking.Status status) {
        return status == Booking.Status.Ongoing;
    }

    @Override
    public void apply(Booking booking, BookingStateContext context) {
        booking.getVehicle().setStatus(Vehicle.Status.Rented);
    }
}
