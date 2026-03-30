package com.rental.controller;

import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.entity.Vehicle;
import com.rental.entity.Brand;
import com.rental.entity.Model;
import com.rental.repository.BrandRepository;
import com.rental.repository.ModelRepository;

import com.rental.dto.*;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.FileService;
import com.rental.service.VehicleService;
import com.rental.repository.VehicleImageRepository;
import com.rental.entity.VehicleImage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final FileService fileService;
    private final VehicleImageRepository vehicleImageRepository;
    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;
    private final com.rental.repository.VehicleRepository vehicleRepository;
    private final com.rental.repository.LocationRepository locationRepository;

    @GetMapping("/dashboard")
    public DashboardStatsDTO dashboard() {
        long totalVehicles = vehicleService.getAllVehicles().size();
        long availableVehicles = vehicleService.getAvailableVehicles().size();
        long pendingBookings = bookingService.getBookingsByStatus(Booking.Status.Pending).size();
        long ongoingBookings = bookingService.getBookingsByStatus(Booking.Status.Ongoing).size();

        return DashboardStatsDTO.builder()
                .totalVehicles(totalVehicles)
                .availableVehicles(availableVehicles)
                .pendingBookings(pendingBookings)
                .ongoingBookings(ongoingBookings)
                .recentBookings(bookingService.getAllBookings().stream()
                        .map(this::convertToBookingDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/bookings")
    public List<BookingDTO> allBookings() {
        return bookingService.getAllBookings().stream()
                .map(this::convertToBookingDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/bookings/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Integer id,
                                               @RequestParam String status,
                                               @RequestParam(required = false) Integer mileage,
                                               @RequestParam(required = false) String note) {
        try {
            bookingService.updateStatus(id, Booking.Status.valueOf(status), mileage, note);
            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật trạng thái thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/vehicles")
    public List<VehicleDTO> allVehicles() {
        return vehicleService.getAllVehicles().stream()
                .map(this::convertToVehicleDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/vehicles/{id}/status")
    public ResponseEntity<?> updateVehicleStatus(@PathVariable Integer id,
                                               @RequestParam String status) {
        try {
            vehicleService.updateStatus(id, Vehicle.Status.valueOf(status));
            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật trạng thái xe thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/vehicles/{id}/maintenance/complete")
    public ResponseEntity<?> completeMaintenance(@PathVariable Integer id) {
        try {
            vehicleService.markAsMaintained(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xác nhận bảo trì xong!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/customers")
    public List<CustomerDTO> allCustomers() {
        return customerService.getAll().stream()
                .map(this::convertToCustomerDTO)
                .collect(Collectors.toList());
    }

    // Locations (Branches) CRUD
    @GetMapping("/locations")
    public List<LocationDTO> allLocations() {
        return locationRepository.findAll().stream()
                .map(this::convertToLocationDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/locations")
    public ResponseEntity<LocationDTO> createLocation(@RequestBody LocationDTO dto) {
        com.rental.entity.Location location = com.rental.entity.Location.builder()
                .branchName(dto.getBranchName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .phone(dto.getPhone())
                .build();
        com.rental.entity.Location saved = locationRepository.save(location);
        return ResponseEntity.ok(convertToLocationDTO(saved));
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<LocationDTO> updateLocation(@PathVariable Integer id, @RequestBody LocationDTO dto) {
        com.rental.entity.Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        location.setBranchName(dto.getBranchName());
        location.setAddress(dto.getAddress());
        location.setCity(dto.getCity());
        location.setPhone(dto.getPhone());
        com.rental.entity.Location saved = locationRepository.save(location);
        return ResponseEntity.ok(convertToLocationDTO(saved));
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Integer id) {
        try {
            locationRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa chi nhánh thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", "Không thể xóa chi nhánh này vì đang có các xe thuộc về chi nhánh này."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Lỗi khi xóa chi nhánh: " + e.getMessage()));
        }
    }

    // Brands CRUD
    @GetMapping("/brands")
    public List<BrandDTO> allBrands() {
        return vehicleService.getAllBrands().stream()
                .map(b -> BrandDTO.builder().brandId(b.getBrandId()).brandName(b.getBrandName()).build())
                .collect(Collectors.toList());
    }

    @PostMapping("/brands")
    public Brand createBrand(@RequestBody Brand brand) {
        return brandRepository.save(brand);
    }

    @PutMapping("/brands/{id}")
    public Brand updateBrand(@PathVariable Integer id, @RequestBody Brand brand) {
        brand.setBrandId(id);
        return brandRepository.save(brand);
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable Integer id) {
        try {
            brandRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa hãng xe thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", "Không thể xóa hãng xe này vì đang có các mẫu xe hoặc xe liên quan."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Lỗi khi xóa hãng xe: " + e.getMessage()));
        }
    }

    // Models CRUD
    @GetMapping("/models")
    public List<ModelDTO> allModels() {
        return vehicleService.getAllModels().stream()
                .map(m -> ModelDTO.builder()
                        .modelId(m.getModelId())
                        .modelName(m.getModelName())
                        .brandId(m.getBrand().getBrandId())
                        .brandName(m.getBrand().getBrandName())
                        .build())
                .collect(Collectors.toList());
    }

    @PostMapping("/models")
    public Model createModel(@RequestBody ModelDTO dto) {
        Brand brand = vehicleService.getBrandById(dto.getBrandId());
        return modelRepository.save(Model.builder()
                .modelName(dto.getModelName())
                .brand(brand)
                .build());
    }

    @PutMapping("/models/{id}")
    public Model updateModel(@PathVariable Integer id, @RequestBody ModelDTO dto) {
        Brand brand = vehicleService.getBrandById(dto.getBrandId());
        return modelRepository.save(Model.builder()
                .modelId(id)
                .modelName(dto.getModelName())
                .brand(brand)
                .build());
    }

    @DeleteMapping("/models/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable Integer id) {
        try {
            modelRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa mẫu xe thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", "Không thể xóa mẫu xe này vì đang có các xe liên quan đang sử dụng mẫu này."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Lỗi khi xóa mẫu xe: " + e.getMessage()));
        }
    }
    @PostMapping("/vehicles")
    public ResponseEntity<VehicleDTO> createVehicle(@RequestBody VehicleDTO dto) {
        Vehicle vehicle = mapToEntity(dto);
        Vehicle saved = vehicleService.save(vehicle);
        return ResponseEntity.ok(convertToVehicleDTO(saved));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<VehicleDTO> updateVehicle(@PathVariable Integer id, @RequestBody VehicleDTO dto) {
        dto.setVehicleId(id);
        Vehicle updated = mapToEntity(dto);
        Vehicle saved = vehicleService.save(updated);
        return ResponseEntity.ok(convertToVehicleDTO(saved));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Integer id) {
        try {
            Vehicle v = vehicleService.getById(id);
            // Delete all associated images from storage
            for (VehicleImage img : v.getImages()) {
                fileService.deleteFile(img.getImageUrl());
            }
            vehicleService.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa xe thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", "Không thể xóa xe này vì đang có các lịch đặt xe (Booking) hoặc lịch sử liên quan."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Lỗi khi xóa xe: " + e.getMessage()));
        }
    }

    @PostMapping("/vehicles/{id}/images")
    public ResponseEntity<?> uploadImages(@PathVariable Integer id, @RequestParam("files") MultipartFile[] files) {
        Vehicle vehicle = vehicleService.getById(id);
        try {
            for (MultipartFile file : files) {
                String url = fileService.saveFile(file, "vehicles");
                VehicleImage img = VehicleImage.builder()
                        .vehicle(vehicle)
                        .imageUrl(url)
                        .isPrimary(vehicle.getImages().isEmpty()) // First image is primary
                        .build();
                vehicleImageRepository.save(img);
            }
            return ResponseEntity.ok("Tải ảnh lên thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tải ảnh: " + e.getMessage());
        }
    }

    @DeleteMapping("/vehicles/images/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable Integer imageId) {
        VehicleImage img = vehicleImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));
        fileService.deleteFile(img.getImageUrl());
        vehicleImageRepository.delete(img);
        return ResponseEntity.ok("Đã xóa ảnh!");
    }

    @PutMapping("/vehicles/images/{imageId}/primary")
    public ResponseEntity<?> setPrimaryImage(@PathVariable Integer imageId) {
        VehicleImage img = vehicleImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));
        
        // Unset other primary images for this vehicle
        List<VehicleImage> images = vehicleImageRepository.findByVehicle(img.getVehicle());
        for (VehicleImage i : images) {
            i.setIsPrimary(i.getImageId().equals(imageId));
        }
        vehicleImageRepository.saveAll(images);
        return ResponseEntity.ok("Đã đặt làm ảnh chính!");
    }

    private Vehicle mapToEntity(VehicleDTO dto) {
        Vehicle v = (dto.getVehicleId() != null) ? vehicleService.getById(dto.getVehicleId()) : new Vehicle();
        v.setLicensePlate(dto.getLicensePlate());
        
        if (dto.getModelId() != null && dto.getModelId() > 0) {
            v.setModel(vehicleService.getModelById(dto.getModelId()));
        } else if (v.getModel() == null) {
            throw new RuntimeException("Vui lòng chọn mẫu xe hợp lệ");
        }
        
        v.setManufactureYear(dto.getYear() != null ? dto.getYear() : (dto.getManufactureYear() != null ? dto.getManufactureYear() : 2023));
        v.setMileage(dto.getMileage() != null ? dto.getMileage() : 0);
        v.setLastMaintenanceMileage(dto.getLastMaintenanceMileage() != null ? dto.getLastMaintenanceMileage() : 0);
        
        java.math.BigDecimal price = dto.getDailyRate() != null ? dto.getDailyRate() : dto.getPricePerDay();
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá thuê mỗi ngày phải lớn hơn 0");
        }
        v.setPricePerDay(price);
        
        v.setSeats(dto.getSeats() != null ? dto.getSeats() : 4);
        v.setFuelType(dto.getFuel() != null ? dto.getFuel() : (dto.getFuelType() != null ? dto.getFuelType() : "Xăng"));
        v.setTransmission(dto.getTransmission() != null ? dto.getTransmission() : "Tự động");
        
        if (dto.getVehicleId() == null) {
            v.setStatus(Vehicle.Status.Available);
        }

        if (dto.getType() != null) {
            v.setType(dto.getType());
        } else if (v.getType() == null) {
            v.setType("Xe ô tô");
        }
        
        if (dto.getLocationId() != null && dto.getLocationId() > 0) {
            v.setCurrentLocation(vehicleService.getLocationById(dto.getLocationId()));
        }
        
        return v;
    }

    private BookingDTO convertToBookingDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setVehicleId(booking.getVehicle().getVehicleId());
        dto.setVehicleModel(booking.getVehicle().getName());
        dto.setCustomerName(booking.getCustomer().getName());
        dto.setCustomerPhone(booking.getCustomer().getPhone());
        dto.setCustomerEmail(booking.getCustomer().getEmail());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setStatus(booking.getStatus());
        dto.setReturnMileage(booking.getReturnMileage());
        dto.setReturnNote(booking.getReturnNote());
        
        if (booking.getPickupLocation() != null) {
            dto.setPickupLocationName(booking.getPickupLocation().getBranchName());
        }
        if (booking.getReturnLocation() != null) {
            dto.setReturnLocationName(booking.getReturnLocation().getBranchName());
        }
        
        return dto;
    }

    private VehicleDTO convertToVehicleDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getVehicleId());
        dto.setVehicleId(vehicle.getVehicleId());
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
        
        dto.setManufactureYear(vehicle.getManufactureYear());
        dto.setYear(vehicle.getManufactureYear());
        dto.setMileage(vehicle.getMileage());
        dto.setLastMaintenanceMileage(vehicle.getLastMaintenanceMileage());
        dto.setPricePerDay(vehicle.getPricePerDay());
        dto.setDailyRate(vehicle.getPricePerDay());
        dto.setStatus(vehicle.getStatus());
        dto.setAverageRating(vehicle.getAverageRating());
        
        if (vehicle.getType() != null) {
            dto.setType(vehicle.getType());
        }
        
        if (vehicle.getCurrentLocation() != null) {
            dto.setLocationId(vehicle.getCurrentLocation().getLocationId());
            dto.setLocation(vehicle.getCurrentLocation().getBranchName());
            dto.setLocationName(vehicle.getCurrentLocation().getBranchName());
        }
        
        dto.setSeats(vehicle.getSeats());
        dto.setFuelType(vehicle.getFuelType());
        dto.setFuel(vehicle.getFuelType());
        dto.setTransmission(vehicle.getTransmission());
        
        if (vehicle.getImages() != null && !vehicle.getImages().isEmpty()) {
            List<VehicleImageDTO> imgDTOs = vehicle.getImages().stream()
                .map(img -> VehicleImageDTO.builder()
                    .imageId(img.getImageId())
                    .imageUrl(img.getImageUrl())
                    .isPrimary(img.getIsPrimary())
                    .build())
                .collect(Collectors.toList());
            dto.setImages(imgDTOs);
            
            // Set primary image URL for main display
            String primaryUrl = imgDTOs.stream()
                .filter(VehicleImageDTO::getIsPrimary)
                .map(VehicleImageDTO::getImageUrl)
                .findFirst()
                .orElse(imgDTOs.get(0).getImageUrl());
            dto.setImage(primaryUrl);
        } else {
            dto.setImage("/images/car-toyota-camry.webp");
        }
        
        return dto;
    }

    private CustomerDTO convertToCustomerDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setUserId(customer.getUserId());
        dto.setCustomerId(customer.getCustomerId());
        dto.setName(customer.getName());
        dto.setFullName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setIdentityCard(customer.getIdentityCard());
        dto.setDriverLicense(customer.getDriverLicense());
        if (customer.getDriverLicenseExpiry() != null) {
            dto.setDriverLicenseExpiry(customer.getDriverLicenseExpiry().toString());
        }
        return dto;
    }

    private LocationDTO convertToLocationDTO(com.rental.entity.Location location) {
        LocationDTO dto = new LocationDTO();
        dto.setLocationId(location.getLocationId());
        dto.setBranchName(location.getBranchName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setPhone(location.getPhone());
        
        // Count vehicles in this location
        dto.setVehicleCount(vehicleRepository.countByCurrentLocationLocationId(location.getLocationId()));
        
        return dto;
    }
}
