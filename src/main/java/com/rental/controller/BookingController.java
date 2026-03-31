package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.BookingDTO;
import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.dto.BookingDTO;
import com.rental.dto.BookingRequestDTO;
import com.rental.repository.LocationRepository;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final LocationRepository locationRepository;

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<Page<BookingDTO>>> myBookings(
            @PageableDefault(size = 5) Pageable pageable,
            Authentication authentication) {
        Customer customer = customerService.findByEmail(authentication.getName());
        Page<BookingDTO> bookings = bookingService.getBookingsByCustomer(customer.getCustomerId(), pageable)
                .map(this::convertToDTO);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Lấy danh sách lịch thuê của bạn thành công"));
    }


    @SuppressWarnings("null")
    @PostMapping("/create/{vehicleId}")
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
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
            booking.setPickupLocation(locationRepository.findById(pickupLocationId).orElse(null));
            booking.setReturnLocation(locationRepository.findById(returnLocationId).orElse(null));
            
            Booking saved = bookingService.createBooking(booking);
            BookingDTO dto = convertToDTO(saved);
            
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getBookingId())
                    .toUri();
            
            return ResponseEntity.created(location)
                    .body(ApiResponse.created(dto, "Đặt xe thành công!"));
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<BookingDTO>builder()
                            .success(false)
                            .message("Đặt xe thất bại: " + e.getMessage())
                            .build());
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
