package com.rental.dto;

import com.rental.entity.Booking;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private Integer bookingId;
    private Integer vehicleId;
    private String vehicleName;
    private String vehicleModel; // Added for compatibility
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String pickupLocationName;
    private String returnLocationName;
    private BigDecimal totalPrice;
    private Booking.Status status;
    private Integer returnMileage;
    private String returnNote;
}
