package com.rental.dto;

import com.rental.entity.Vehicle;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleDTO {
    private Integer id; // For frontend compatibility
    private Integer vehicleId;
    private String licensePlate;
    private Integer modelId;
    private String modelName;
    private Integer brandId;
    private String brandName;
    private String name; // Brand + Model + Year
    private String model; // For compatibility, will store model name
    private String brand; // For compatibility, will store brand name
    private Integer manufactureYear;
    private Integer year; // For frontend compatibility
    private Integer mileage;
    private Integer lastMaintenanceMileage;
    private BigDecimal pricePerDay;
    private BigDecimal dailyRate; // For frontend compatibility
    private BigDecimal depositAmount;
    private Vehicle.Status status;
    private String type;
    private Integer locationId;
    private String location; // Location branch name
    private String locationName;
    private Float averageRating;
    private Integer seats;
    private String fuel;
    private String fuelType; // For backend compatibility
    private String transmission;
    private String image;
    private java.util.List<VehicleImageDTO> images;
    private Integer engineCapacity;
}
