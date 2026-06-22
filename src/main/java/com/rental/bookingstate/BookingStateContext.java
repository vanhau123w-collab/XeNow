package com.rental.bookingstate;

public record BookingStateContext(
        Integer mileage,
        String note,
        String returnPaymentMethod
) {
}
