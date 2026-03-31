package com.rental.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequestDTO {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer pickupLocationId;
    private Integer returnLocationId;
}
