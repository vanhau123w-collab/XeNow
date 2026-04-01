package com.rental.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequestDTO {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String pickupAddress;
    private String returnAddress;
    private Integer driverLicenseId;
    private String paymentMethod;
    private java.math.BigDecimal paymentAmount;
}
