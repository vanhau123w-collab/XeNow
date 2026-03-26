package com.rental.controller;

import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.VehicleService;
import com.rental.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final LocationRepository locationRepository;

    @GetMapping("/create/{vehicleId}")
    public String showBookingForm(@PathVariable Integer vehicleId, Model model) {
        model.addAttribute("vehicle", vehicleService.getById(vehicleId));
        model.addAttribute("locations", locationRepository.findAll());
        model.addAttribute("booking", new Booking());
        return "booking-form";
    }

    @PostMapping("/create/{vehicleId}")
    public String createBooking(
            @PathVariable Integer vehicleId,
            @ModelAttribute Booking booking,
            @RequestParam Integer pickupLocationId,
            @RequestParam Integer returnLocationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Customer customer = customerService.findByEmail(authentication.getName());
            booking.setCustomer(customer);
            booking.setVehicle(vehicleService.getById(vehicleId));
            booking.setPickupLocation(locationRepository.findById(pickupLocationId).orElse(null));
            booking.setReturnLocation(locationRepository.findById(returnLocationId).orElse(null));
            bookingService.createBooking(booking);
            redirectAttributes.addFlashAttribute("success", "Đặt xe thành công! Chờ xác nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Authentication authentication, Model model) {
        Customer customer = customerService.findByEmail(authentication.getName());
        model.addAttribute("bookings", bookingService.getBookingsByCustomer(customer.getCustomerId()));
        return "my-bookings";
    }
}
