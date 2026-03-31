package com.rental.service;

import com.rental.entity.Booking;
import com.rental.entity.Vehicle;
import com.rental.entity.Customer;
import com.rental.repository.BookingRepository;
import com.rental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BookingService {


    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByStartDateDesc();
    }

    public List<Booking> getBookingsByCustomer(Integer userId) {
        return bookingRepository.findByCustomerUserId(userId);
    }

    public List<Booking> getBookingsByStatus(Booking.Status status) {
        return bookingRepository.findByStatus(status);
    }

    public Booking getById(Integer id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt xe với ID: " + id));
    }

    @Transactional
    public Booking createBooking(Booking booking) {
        // Validate customer verification
        Customer customer = booking.getCustomer();
        if (customer.getIdentityCard() == null || customer.getIdentityCard().isEmpty() ||
            customer.getDriverLicense() == null || customer.getDriverLicense().isEmpty()) {
            throw new RuntimeException("Tài khoản chưa được xác thực thông tin định danh (CCCD/GPLX). Vui lòng xác thực trước khi đặt xe.");
        }

        long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
        if (days <= 0) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận xe ít nhất 1 ngày");

        Vehicle vehicle = booking.getVehicle();
        BigDecimal pricePerDay = vehicle.getPricePerDay();
        booking.setTotalPrice(pricePerDay.multiply(BigDecimal.valueOf(days)));
        
        // Inherit deposit amount from vehicle
        booking.setDepositAmount(vehicle.getDepositAmount() != null ? vehicle.getDepositAmount() : BigDecimal.ZERO);
        
        booking.setStatus(Booking.Status.Pending);
        return bookingRepository.save(booking);
    }

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

    @Transactional
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus) {
        return updateStatus(bookingId, newStatus, null, null);
    }
    List<Booking> getAllBookings();
    Page<Booking> getAllBookings(Pageable pageable);
    List<Booking> getBookingsByCustomer(Integer userId);
    Page<Booking> getBookingsByCustomer(Integer userId, Pageable pageable);
    List<Booking> getBookingsByStatus(Booking.Status status);
    Page<Booking> getBookingsByStatus(Booking.Status status, Pageable pageable);
    Booking getById(Integer id);
    Booking createBooking(Booking booking);
    Booking updateStatus(Integer bookingId, Booking.Status newStatus, Integer mileage, String note);
    Booking updateStatus(Integer bookingId, Booking.Status newStatus);
}

