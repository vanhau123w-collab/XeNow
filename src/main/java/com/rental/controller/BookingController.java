package com.rental.controller;

import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.dto.BookingDTO;
import com.rental.dto.BookingRequestDTO;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.VehicleService;
import com.rental.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final LocationRepository locationRepository;

    @GetMapping("/my-bookings")
    public List<BookingDTO> myBookings(Authentication authentication) {
        Customer customer = customerService.findByEmail(authentication.getName());
        return bookingService.getBookingsByCustomer(customer.getCustomerId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/create/{vehicleId}")
    public ResponseEntity<?> createBooking(
            @PathVariable Integer vehicleId,
            @RequestBody BookingRequestDTO request,
            Authentication authentication) {
        try {
            Customer customer = customerService.findByEmail(authentication.getName());
            
            Booking booking = new Booking();
            booking.setCustomer(customer);
            booking.setVehicle(vehicleService.getById(vehicleId));
            booking.setStartDate(request.getStartDate());
            booking.setEndDate(request.getEndDate());
            
            if (request.getPickupLocationId() != null) {
                booking.setPickupLocation(locationRepository.findById(request.getPickupLocationId()).orElse(null));
            }
            if (request.getReturnLocationId() != null) {
                booking.setReturnLocation(locationRepository.findById(request.getReturnLocationId()).orElse(null));
            }
            
            Booking saved = bookingService.createBooking(booking);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for internal context
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "Lỗi tạo đơn đặt xe: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<?> confirmPayment(@PathVariable Integer id) {
        try {
            Booking booking = bookingService.updateStatus(id, Booking.Status.Confirmed);
            return ResponseEntity.ok(convertToDTO(booking));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }

    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setVehicleId(booking.getVehicle().getVehicleId());
        dto.setVehicleModel(booking.getVehicle().getName());
        dto.setCustomerName(booking.getCustomer().getName());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        if (booking.getPickupLocation() != null) {
            dto.setPickupLocationName(booking.getPickupLocation().getAddress());
        }
        if (booking.getReturnLocation() != null) {
            dto.setReturnLocationName(booking.getReturnLocation().getAddress());
        }
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setDepositAmount(booking.getDepositAmount());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        return dto;
    }
}
