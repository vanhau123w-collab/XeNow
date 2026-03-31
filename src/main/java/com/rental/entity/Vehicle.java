package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    public enum Status {
        Available, Rented, Maintenance, Broken
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Integer vehicleId;

    @Column(name = "Type", length = 50)
    private String type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CurrentLocationID")
    private Location currentLocation;

    @Column(name = "LicensePlate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "ModelID")
    private Model model;

    @Column(name = "ManufactureYear")
    private Integer manufactureYear;
    
    // Getter methods for compatibility
    public String getModelName() {
        return model != null ? model.getModelName() : "N/A";
    }

    public String getBrandName() {
        return (model != null && model.getBrand() != null) ? model.getBrand().getBrandName() : "N/A";
    }
    
    // Virtual name generated as requested: Brand + Model + Year
    public String getFullName() {
        String mName = getModelName();
        String bName = getBrandName();
        if (mName != null && bName != null) {
            String mLower = mName.toLowerCase();
            String bLower = bName.toLowerCase();
            if (mLower.contains(bLower)) {
                return mName + " " + (manufactureYear != null ? manufactureYear : "");
            }
        }
        return bName + " " + mName + " " + (manufactureYear != null ? manufactureYear : "");
    }

    public String getName() {
        return getFullName();
    }
    
    public Integer getYearMade() {
        return manufactureYear;
    }
    
    public String getColor() {
        return "N/A"; // Default value, add color field if needed
    }
    
    public BigDecimal getDailyRate() {
        return pricePerDay;
    }

    @Column(name = "Mileage")
    @Builder.Default
    private Integer mileage = 0;

    @Column(name = "LastMaintenanceMileage")
    @Builder.Default
    private Integer lastMaintenanceMileage = 0;

    @Column(name = "Seats")
    private Integer seats;

    @Column(name = "FuelType", length = 50)
    private String fuelType;

    @Column(name = "Transmission", length = 50)
    private String transmission;

    @Column(name = "PricePerDay", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "DepositAmount", precision = 15, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    @Builder.Default
    private Status status = Status.Available;

    @Column(name = "AverageRating")
    @Builder.Default
    private Float averageRating = 0f;

    @Column(name = "TotalReviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private java.util.List<VehicleImage> images = new java.util.ArrayList<>();

    @Column(name = "DeletedAt")
    private LocalDateTime deletedAt;
}
