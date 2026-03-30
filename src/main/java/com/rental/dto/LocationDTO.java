package com.rental.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private Integer locationId;
    private String branchName;
    private String address;
    private String city;
    private String phone;
    private long vehicleCount;
}
