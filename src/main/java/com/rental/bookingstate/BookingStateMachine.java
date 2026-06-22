package com.rental.bookingstate;

import com.rental.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingStateMachine {

    private final List<BookingStateHandler> handlers;

    public void transitionTo(Booking booking, Booking.Status newStatus, BookingStateContext context) {
        booking.setStatus(newStatus);
        handlers.stream()
                .filter(handler -> handler.supports(newStatus))
                .findFirst()
                .ifPresent(handler -> handler.apply(booking, context));
    }
}
