package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "DriverLicense")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LicenseID")
    private Integer licenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private Customer customer;

    @Column(name = "LicenseClass", length = 10, nullable = false)
    private String licenseClass;

    @Column(name = "LicenseNumber", length = 50, nullable = false, unique = true)
    private String licenseNumber;

    @Column(name = "IssueDate", nullable = false)
    private LocalDate issueDate;

    @Column(name = "ExpiryDate")
    private LocalDate expiryDate;

    @Column(name = "ImageUrl", length = 500)
    private String imageUrl;
}
