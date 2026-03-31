package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.VehicleDTO;
import com.rental.dto.VehicleImageDTO;
import com.rental.entity.Vehicle;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VehicleDTO>>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String seats,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String transmission,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("vehicleId").descending());
        
        List<Vehicle> allVehicles = vehicleService.getAvailableVehicles();
        
        // Apply filters
        List<Vehicle> filtered = allVehicles.stream()
            .filter(v -> type == null || type.isEmpty() || type.equals(v.getType()))
            .filter(v -> brand == null || brand.isEmpty() || 
                java.util.Arrays.asList(brand.split(",")).contains(v.getBrandName()))
            .filter(v -> seats == null || seats.isEmpty() || 
                java.util.Arrays.asList(seats.split(",")).stream()
                    .anyMatch(s -> s.equals(String.valueOf(v.getSeats()))))
            .filter(v -> location == null || location.isEmpty() || 
                (v.getCurrentLocation() != null && 
                java.util.Arrays.asList(location.split(",")).contains(v.getCurrentLocation().getBranchName())))
            .filter(v -> transmission == null || transmission.isEmpty() || 
                java.util.Arrays.asList(transmission.split(",")).contains(v.getTransmission()))
            .collect(java.util.stream.Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<VehicleDTO> dtos = filtered.subList(start > filtered.size() ? filtered.size() : start, end)
            .stream().map(this::convertToDTO).collect(java.util.stream.Collectors.toList());
        
        Page<VehicleDTO> result = new org.springframework.data.domain.PageImpl<>(dtos, pageable, filtered.size());
        
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách xe thành công"));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleDTO>> getById(@PathVariable Integer id) {
        Vehicle vehicle = vehicleService.getById(id);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Không tìm thấy xe với mã: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(vehicle), "Lấy thông tin xe thành công"));
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
        dto.setDepositAmount(vehicle.getDepositAmount());
        dto.setStatus(vehicle.getStatus());
        dto.setMileage(vehicle.getMileage());
        dto.setAverageRating(vehicle.getAverageRating());
        
        // Type
        if (vehicle.getType() != null) {
            dto.setType(vehicle.getType());
        }
        
        // Fuel and transmission
        dto.setFuelType(vehicle.getFuelType());
        dto.setFuel(vehicle.getFuelType());
        dto.setTransmission(vehicle.getTransmission());
        
        // Location
        if (vehicle.getCurrentLocation() != null) {
            String locationName = vehicle.getCurrentLocation().getBranchName();
            dto.setLocationId(vehicle.getCurrentLocation().getLocationId());
            dto.setLocation(locationName);
            dto.setLocationName(locationName);
        }
        
        // Images logic from AdminController
        if (vehicle.getImages() != null && !vehicle.getImages().isEmpty()) {
            List<com.rental.dto.VehicleImageDTO> imgDTOs = vehicle.getImages().stream()
                .map(img -> com.rental.dto.VehicleImageDTO.builder()
                    .imageId(img.getImageId())
                    .imageUrl(img.getImageUrl())
                    .isPrimary(img.getIsPrimary())
        // Images from database
        if (vehicle.getImages() != null && !vehicle.getImages().isEmpty()) {
            List<VehicleImageDTO> imgDTOs = vehicle.getImages().stream()
                .map(img -> VehicleImageDTO.builder()
                    .imageId(img.getImageId())
                    .imageUrl(img.getImageUrl())
                    .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                    .build())
                .collect(Collectors.toList());
            dto.setImages(imgDTOs);
            
            String primaryUrl = imgDTOs.stream()
                .filter(com.rental.dto.VehicleImageDTO::getIsPrimary)
                .map(com.rental.dto.VehicleImageDTO::getImageUrl)
                .findFirst()
                .orElse(imgDTOs.get(0).getImageUrl());
            // Set primary image URL for main display
            String primaryUrl = imgDTOs.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(VehicleImageDTO::getImageUrl)
                .findFirst()
                .orElse(imgDTOs.isEmpty() ? "/images/car-toyota-camry.webp" : imgDTOs.get(0).getImageUrl());
            dto.setImage(primaryUrl);
        } else {
            dto.setImage("/images/car-toyota-camry.webp");
        }
        
        return dto;
    }
}
