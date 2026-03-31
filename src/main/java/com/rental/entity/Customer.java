package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "Customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @Column(name = "UserID")
    private Integer userId;
    
    // Getter methods for User properties
    public Integer getCustomerId() {
        return userId;
    }
    
    public String getName() {
        return user != null ? user.getFullName() : null;
    }
    
    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }
    
    public String getPhone() {
        return user != null ? user.getPhone() : null;
    }

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "UserID")
    private User user;

    @Column(name = "IdentityCard", nullable = false, unique = true, length = 20)
    private String identityCard;

    @Column(name = "IdentityCardIssueDate")
    private LocalDate identityCardIssueDate;

    @Column(name = "IdentityCardExpiry")
    private LocalDate identityCardExpiry;

    @Column(name = "Address", length = 255)
    private String address;

    @Column(name = "DriverLicense", nullable = false, unique = true, length = 20)
    private String driverLicense;

    @Column(name = "DriverLicenseClass", length = 10)
    private String driverLicenseClass;

    @Column(name = "DriverLicenseIssueDate")
    private LocalDate driverLicenseIssueDate;

    @Column(name = "DriverLicenseExpiry")
    private LocalDate driverLicenseExpiry;
}
