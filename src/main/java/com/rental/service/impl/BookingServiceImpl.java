package com.rental.service.impl;

import com.rental.bookingstate.BookingStateContext;
import com.rental.bookingstate.BookingStateMachine;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingStateMachine bookingStateMachine;

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
        Vehicle vehicle = vehicleRepository.findById(booking.getVehicle().getVehicleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));

        // 1. Kiểm tra trạng thái xe
        if (vehicle.getStatus() == Vehicle.Status.Maintenance) {
            throw new RuntimeException("Phương tiện hiện đang trong quá trình bảo trì, không thể đặt xe.");
        }
        if (vehicle.getStatus() == Vehicle.Status.Broken) {
            throw new RuntimeException("Phương tiện hiện đang gặp sự cố kỹ thuật, không thể đặt xe.");
        }

        // 2. Kiểm tra trùng lịch (Overlapping bookings)
        List<Booking.Status> activeStatuses = List.of(Booking.Status.Confirmed, Booking.Status.Ongoing);
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                vehicle.getVehicleId(),
                booking.getStartDate(),
                booking.getEndDate(),
                activeStatuses);

        if (!overlaps.isEmpty()) {
            throw new RuntimeException(
                    "Phương tiện đã được thuê hoặc có lịch đặt trong khoảng thời gian này. Vui lòng chọn thời gian khác hoặc xe khác.");
        }

        long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
        if (days <= 0)
            throw new IllegalArgumentException("Ngày trả phải sau ngày nhận xe");

        BigDecimal pricePerDay = vehicle.getPricePerDay();
        booking.setTotalPrice(pricePerDay.multiply(BigDecimal.valueOf(days)));
        booking.setStatus(Booking.Status.Pending);
        booking.setVehicle(vehicle); // Ensure we use the managed entity
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus, Integer mileage, String note,
            String returnPaymentMethod) {
        Booking booking = getById(bookingId);
        bookingStateMachine.transitionTo(
                booking,
                newStatus,
                new BookingStateContext(mileage, note, returnPaymentMethod));

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updateStatus(Integer bookingId, Booking.Status newStatus) {
        return updateStatus(bookingId, newStatus, null, null, null);
    }
}
