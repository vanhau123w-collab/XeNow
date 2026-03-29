package com.rental.controller;

import com.rental.entity.Vehicle;
import com.rental.dto.VehicleDTO;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public List<VehicleDTO> getAll(@RequestParam(required = false) String type) {
        if (type != null) {
            return vehicleService.getAvailableByType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
        return vehicleService.getAvailableVehicles().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getById(@PathVariable Integer id) {
        Vehicle vehicle = vehicleService.getById(id);
        if (vehicle == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(convertToDTO(vehicle));
    }

    private VehicleDTO convertToDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        
        // IDs
        dto.setId(vehicle.getVehicleId());
        dto.setVehicleId(vehicle.getVehicleId());
        
        // Basic info
        dto.setLicensePlate(vehicle.getLicensePlate());
        // Dynamic name generation
        dto.setName(vehicle.getName());
        dto.setModelName(vehicle.getModelName());
        dto.setBrandName(vehicle.getBrandName());
        dto.setModel(vehicle.getModelName()); // For compatibility
        dto.setBrand(vehicle.getBrandName()); // For compatibility
        
        if (vehicle.getModel() != null) {
            dto.setModelId(vehicle.getModel().getModelId());
        }
        
        // Year
        dto.setYear(vehicle.getManufactureYear());
        dto.setManufactureYear(vehicle.getManufactureYear());
        
        // Pricing and status
        dto.setPricePerDay(vehicle.getPricePerDay());
        dto.setDailyRate(vehicle.getPricePerDay());
        dto.setStatus(vehicle.getStatus());
        dto.setMileage(vehicle.getMileage());
        dto.setAverageRating(vehicle.getAverageRating());
        
        // Type
        if (vehicle.getType() != null) {
            dto.setType(vehicle.getType());
        }
        
        // Vehicle specs from database
        dto.setSeats(vehicle.getSeats());
        dto.setFuel(vehicle.getFuelType());
        dto.setTransmission(vehicle.getTransmission());
        
        // Location
        if (vehicle.getCurrentLocation() != null) {
            String locationName = vehicle.getCurrentLocation().getBranchName();
            dto.setLocation(locationName);
            dto.setLocationName(locationName);
        }
        
        // Image - placeholder for now
        dto.setImage("/images/car-toyota-camry.webp");
        
        return dto;
    }
}
