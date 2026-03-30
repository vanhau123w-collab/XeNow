package com.rental.service;

import com.rental.entity.Booking;
import java.util.List;

public interface BookingService {
    List<Booking> getAllBookings();
    List<Booking> getBookingsByCustomer(Integer userId);
    List<Booking> getBookingsByStatus(Booking.Status status);
    Booking getById(Integer id);
    Booking createBooking(Booking booking);
    Booking updateStatus(Integer bookingId, Booking.Status newStatus, Integer mileage, String note);
    Booking updateStatus(Integer bookingId, Booking.Status newStatus);
}
