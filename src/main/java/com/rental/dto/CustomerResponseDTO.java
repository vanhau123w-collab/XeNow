package com.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDTO {
    private Integer userId;
    private String identityCard;
    private String driverLicense;
    private LocalDate driverLicenseExpiry;
    private String address;
    private String driverLicenseClass;
    private LocalDate identityCardIssueDate;
    private LocalDate identityCardExpiry;
    private LocalDate driverLicenseIssueDate;
}
