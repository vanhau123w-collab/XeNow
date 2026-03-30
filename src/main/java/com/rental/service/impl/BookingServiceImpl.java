package com.rental.service.impl;

import com.rental.entity.Booking;
import com.rental.entity.Vehicle;
import com.rental.repository.BookingRepository;
import com.rental.repository.VehicleRepository;
import com.rental.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByStartDateDesc();
    }

    @Override
    public List<Booking> getBookingsByCustomer(Integer userId) {
        return bookingRepository.findByCustomerUserId(userId);
    }

    @Override
    public List<Booking> getBookingsByStatus(Booking.Status status) {
        return bookingRepository.findByStatus(status);
    }

    @Override
    public Booking getById(Integer id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt xe với ID: " + id));
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking) {
        long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
        if (days <= 0) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận xe");

        BigDecimal pricePerDay = booking.getVehicle().getPricePerDay();
        booking.setTotalPrice(pricePerDay.multiply(BigDecimal.valueOf(days)));
        booking.setStatus(Booking.Status.Pending);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus, Integer mileage, String note) {
        Booking booking = getById(bookingId);
        booking.setStatus(newStatus);

        Vehicle vehicle = booking.getVehicle();
        
        if (newStatus == Booking.Status.Ongoing) {
            vehicle.setStatus(Vehicle.Status.Rented);
            vehicleRepository.save(vehicle);
        } else if (newStatus == Booking.Status.Completed) {
            vehicle.setStatus(Vehicle.Status.Available);
            if (mileage != null && mileage > vehicle.getMileage()) {
                vehicle.setMileage(mileage);
            }
            vehicleRepository.save(vehicle);
            
            booking.setReturnMileage(mileage);
            booking.setReturnNote(note);
        } else if (newStatus == Booking.Status.Cancelled) {
            vehicle.setStatus(Vehicle.Status.Available);
            vehicleRepository.save(vehicle);
        }

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus) {
        return updateStatus(bookingId, newStatus, null, null);
    }
}
