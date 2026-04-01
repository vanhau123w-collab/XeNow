package com.rental.service.impl;

import com.rental.entity.Booking;
import com.rental.entity.Vehicle;
import com.rental.repository.BookingRepository;
import com.rental.repository.VehicleRepository;
import com.rental.repository.PaymentRepository;
import com.rental.entity.Payment;
import com.rental.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByStartDateDesc();
    }

    @Override
    public Page<Booking> getAllBookings(Pageable pageable) {
        return bookingRepository.findAllByOrderByStartDateDesc(pageable);
    }

    @Override
    public List<Booking> getBookingsByCustomer(Integer userId) {
        return bookingRepository.findByCustomer_UserIdOrderByStartDateDesc(userId);
    }

    @Override
    public Page<Booking> getBookingsByCustomer(Integer userId, Pageable pageable) {
        return bookingRepository.findByCustomer_UserIdOrderByStartDateDesc(userId, pageable);
    }

    @Override
    public List<Booking> getBookingsByStatus(Booking.Status status) {
        return bookingRepository.findByStatus(status);
    }

    @Override
    public Page<Booking> getBookingsByStatus(Booking.Status status, Pageable pageable) {
        return bookingRepository.findByStatus(status, pageable);
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
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus, Integer mileage, String note, String returnPaymentMethod) {
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
            
            // Generate payment for remainder if returnPaymentMethod exists
            if (returnPaymentMethod != null && !returnPaymentMethod.isEmpty()) {
                // Calculate remainder
                BigDecimal total = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
                List<Payment> existingPayments = paymentRepository.findByBookingBookingId(bookingId);
                BigDecimal paidAmount = existingPayments.stream()
                        .filter(p -> p.getStatus() == Payment.Status.Completed || p.getStatus() == Payment.Status.Pending) // Assume pending will complete
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal remainder = total.subtract(paidAmount);
                if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                    Payment payment = new Payment();
                    payment.setBooking(booking);
                    payment.setAmount(remainder);
                    if ("Chuyển khoản".equalsIgnoreCase(returnPaymentMethod)) {
                        payment.setPaymentMethod(Payment.Method.BankTransfer);
                    } else if ("Thẻ tín dụng".equalsIgnoreCase(returnPaymentMethod)) {
                        payment.setPaymentMethod(Payment.Method.CreditCard);
                    } else {
                        payment.setPaymentMethod(Payment.Method.Cash);
                    }
                    payment.setStatus(Payment.Status.Completed);
                    paymentRepository.save(payment);
                }
            }
        } else if (newStatus == Booking.Status.Cancelled) {
            vehicle.setStatus(Vehicle.Status.Available);
            vehicleRepository.save(vehicle);
        }

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus) {
        return updateStatus(bookingId, newStatus, null, null, null);
    }
}
