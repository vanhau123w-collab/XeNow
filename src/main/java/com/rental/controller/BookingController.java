package com.rental.controller;

import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.dto.BookingDTO;
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

    @SuppressWarnings("null")
    @PostMapping("/create/{vehicleId}")
    public ResponseEntity<?> createBooking(
            @PathVariable Integer vehicleId,
            @RequestBody Booking booking,
            @RequestParam Integer pickupLocationId,
            @RequestParam Integer returnLocationId,
            Authentication authentication) {
        try {
            Customer customer = customerService.findByEmail(authentication.getName());
            booking.setCustomer(customer);
            booking.setVehicle(vehicleService.getById(vehicleId));
            booking.setPickupLocation(locationRepository.findById(pickupLocationId).orElse(null));
            booking.setReturnLocation(locationRepository.findById(returnLocationId).orElse(null));
            Booking saved = bookingService.createBooking(booking);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
        dto.setStatus(booking.getStatus());
        return dto;
    }
}
