package com.rental.controller;

import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.entity.Vehicle;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalVehicles = vehicleService.getAllVehicles().size();
        long availableVehicles = vehicleService.getAvailableVehicles().size();
        long pendingBookings = bookingService.getBookingsByStatus(Booking.Status.Pending).size();
        long ongoingBookings = bookingService.getBookingsByStatus(Booking.Status.Ongoing).size();

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("availableVehicles", availableVehicles);
        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("ongoingBookings", ongoingBookings);
        model.addAttribute("recentBookings", bookingService.getAllBookings());
        return "admin/dashboard";
    }

    @GetMapping("/bookings")
    public String allBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/bookings";
    }

    @PostMapping("/bookings/{id}/status")
    public String updateBookingStatus(@PathVariable Integer id,
                                       @RequestParam String status,
                                       RedirectAttributes redirectAttributes) {
        try {
            bookingService.updateStatus(id, Booking.Status.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/vehicles")
    public String allVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/vehicles";
    }

    @PostMapping("/vehicles/{id}/status")
    public String updateVehicleStatus(@PathVariable Integer id,
                                       @RequestParam String status,
                                       RedirectAttributes redirectAttributes) {
        try {
            vehicleService.updateStatus(id, Vehicle.Status.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái xe thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @GetMapping("/customers")
    public String allCustomers(Model model) {
        model.addAttribute("customers", customerService.getAll());
        return "admin/customers";
    }
}
