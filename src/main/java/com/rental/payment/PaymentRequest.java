package com.rental.payment;

import com.rental.entity.Booking;
import com.rental.entity.Vehicle;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

public record PaymentRequest(
        Booking booking,
        Vehicle vehicle,
        BigDecimal amount,
        String method,
        HttpServletRequest httpRequest
) {
}
