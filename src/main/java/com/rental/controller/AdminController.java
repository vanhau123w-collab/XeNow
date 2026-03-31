package com.rental.controller;

import com.rental.dto.*;
import com.rental.entity.*;
import com.rental.repository.BrandRepository;
import com.rental.repository.ModelRepository;
import com.rental.repository.VehicleImageRepository;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.FileService;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.net.URI;
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
    private final com.rental.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.transaction.annotation.Transactional
    @GetMapping("/rescue-password")
    public ResponseEntity<?> rescuePassword() {
        try {
            com.rental.entity.User user = userRepository.findById(3)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User ID 3"));
            String oldPass = user.getPassword();
            String newHash = passwordEncoder.encode("123456");
            user.setPassword(newHash);
            userRepository.saveAndFlush(user);
            
            System.out.println("USER ID 3: " + user.getUsername() + " | FullName: " + user.getFullName());
            System.out.println("OLD PASS: " + oldPass);
            System.out.println("NEW HASH: " + newHash);
            
            return ResponseEntity.ok("Mật khẩu của " + user.getUsername() + " (ID 3) đã được đổi thành: 123456. Hash: " + newHash);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> dashboard() {
        long totalVehicles = vehicleService.getAllVehicles().size();
        long availableVehicles = vehicleService.getAvailableVehicles().size();
        long pendingBookings = bookingService.getBookingsByStatus(Booking.Status.Pending).size();
        long ongoingBookings = bookingService.getBookingsByStatus(Booking.Status.Ongoing).size();

        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalVehicles(totalVehicles)
                .availableVehicles(availableVehicles)
                .pendingBookings(pendingBookings)
                .ongoingBookings(ongoingBookings)
                .recentBookings(bookingService.getAllBookings().stream()
                        .map(this::convertToBookingDTO)
                        .collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(ApiResponse.success(stats, "Tải dữ liệu Dashboard thành công"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Page<BookingDTO>>> allBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("bookingId").descending());
        Page<BookingDTO> bookings = bookingService.getAllBookings(pageable)
                .map(this::convertToBookingDTO);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Lấy danh sách đặt xe thành công"));
    }

    @PostMapping("/bookings/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateBookingStatus(@PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) Integer mileage,
            @RequestParam(required = false) String note) {
        try {
            bookingService.updateStatus(id, Booking.Status.valueOf(status), mileage, note);
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<Page<VehicleDTO>>> allVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("vehicleId").descending());
        Page<VehicleDTO> vehicles = vehicleService.getAllVehicles(pageable)
                .map(this::convertToVehicleDTO);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Lấy danh sách xe thành công"));
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleDTO>> getVehicleById(@PathVariable Integer id) {
        Vehicle vehicle = vehicleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToVehicleDTO(vehicle), "Lấy thông tin xe thành công"));
    }

    @PostMapping("/vehicles/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateVehicleStatus(@PathVariable Integer id,
            @RequestParam String status) {
        try {
            vehicleService.updateStatus(id, Vehicle.Status.valueOf(status));
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái xe thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/vehicles/{id}/maintenance/complete")
    public ResponseEntity<ApiResponse<Object>> completeMaintenance(@PathVariable Integer id) {
        try {
            vehicleService.markAsMaintained(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Đã xác nhận bảo trì xong!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> allCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("customerId").descending());
        Page<CustomerDTO> customers = customerService.getAll(pageable)
                .map(this::convertToCustomerDTO);
        return ResponseEntity.ok(ApiResponse.success(customers, "Lấy danh sách khách hàng thành công"));
    }


    // Locations (Branches) CRUD
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<Page<LocationDTO>>> allLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("locationId").ascending());
        Page<LocationDTO> locations = locationRepository.findAll(pageable)
                .map(this::convertToLocationDTO);
        return ResponseEntity.ok(ApiResponse.success(locations, "Lấy danh sách chi nhánh thành công"));
    }

    @PostMapping("/locations")
    public ResponseEntity<ApiResponse<LocationDTO>> createLocation(@RequestBody LocationDTO dto) {
        com.rental.entity.Location location = com.rental.entity.Location.builder()
                .branchName(dto.getBranchName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .phone(dto.getPhone())
                .build();
        com.rental.entity.Location saved = locationRepository.save(location);
        LocationDTO result = convertToLocationDTO(saved);
        
        URI locationUri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getLocationId())
                .toUri();
        
        return ResponseEntity.created(locationUri)
                .body(ApiResponse.created(result, "Thêm chi nhánh mới thành công"));
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<ApiResponse<LocationDTO>> updateLocation(@PathVariable Integer id, @RequestBody LocationDTO dto) {
        com.rental.entity.Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        location.setBranchName(dto.getBranchName());
        location.setAddress(dto.getAddress());
        location.setCity(dto.getCity());
        location.setPhone(dto.getPhone());
        com.rental.entity.Location saved = locationRepository.save(location);
        return ResponseEntity.ok(ApiResponse.success(convertToLocationDTO(saved), "Cập nhật chi nhánh thành công"));
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteLocation(@PathVariable Integer id) {
        try {
            locationRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa chi nhánh thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Không thể xóa chi nhánh này vì đang có các xe thuộc về chi nhánh này."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa chi nhánh: " + e.getMessage()));
        }
    }

    // Brands CRUD
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<Page<BrandDTO>>> allBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("brandName").ascending());
        Page<BrandDTO> brands = brandRepository.findAll(pageable)
                .map(b -> BrandDTO.builder().brandId(b.getBrandId()).brandName(b.getBrandName()).build());
        return ResponseEntity.ok(ApiResponse.success(brands, "Lấy danh sách hãng xe thành công"));
    }

    @PostMapping("/brands")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestBody Brand brand) {
        Brand saved = brandRepository.save(brand);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getBrandId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created(saved, "Thêm hãng xe thành công"));
    }

    @PutMapping("/brands/{id}")
    public ResponseEntity<ApiResponse<Brand>> updateBrand(@PathVariable Integer id, @RequestBody Brand brand) {
        brand.setBrandId(id);
        Brand saved = brandRepository.save(brand);
        return ResponseEntity.ok(ApiResponse.success(saved, "Cập nhật hãng xe thành công"));
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteBrand(@PathVariable Integer id) {
        try {
            brandRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa hãng xe thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Không thể xóa hãng xe này vì đang có các mẫu xe hoặc xe liên quan."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa hãng xe: " + e.getMessage()));
        }
    }

    // Models CRUD
    @GetMapping("/models")
    public ResponseEntity<ApiResponse<Page<ModelDTO>>> allModels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("modelName").ascending());
        Page<ModelDTO> models = modelRepository.findAll(pageable)
                .map(m -> ModelDTO.builder()
                        .modelId(m.getModelId())
                        .modelName(m.getModelName())
                        .brandId(m.getBrand().getBrandId())
                        .brandName(m.getBrand().getBrandName())
                        .build());
        return ResponseEntity.ok(ApiResponse.success(models, "Lấy danh sách mẫu xe thành công"));
    }

    @PostMapping("/models")
    public ResponseEntity<ApiResponse<Model>> createModel(@RequestBody ModelDTO dto) {
        Brand brand = vehicleService.getBrandById(dto.getBrandId());
        Model saved = modelRepository.save(Model.builder()
                .modelName(dto.getModelName())
                .brand(brand)
                .build());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getModelId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created(saved, "Thêm mẫu xe thành công"));
    }

    @PutMapping("/models/{id}")
    public ResponseEntity<ApiResponse<Model>> updateModel(@PathVariable Integer id, @RequestBody ModelDTO dto) {
        Brand brand = vehicleService.getBrandById(dto.getBrandId());
        Model saved = modelRepository.save(Model.builder()
                .modelId(id)
                .modelName(dto.getModelName())
                .brand(brand)
                .build());
        return ResponseEntity.ok(ApiResponse.success(saved, "Cập nhật mẫu xe thành công"));
    }

    @DeleteMapping("/models/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteModel(@PathVariable Integer id) {
        try {
            modelRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa mẫu xe thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Không thể xóa mẫu xe này vì đang có các xe liên quan đang sử dụng mẫu này."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa mẫu xe: " + e.getMessage()));
        }
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse<VehicleDTO>> createVehicle(@RequestBody VehicleDTO dto) {
        Vehicle vehicle = mapToEntity(dto);
        Vehicle saved = vehicleService.save(vehicle);
        VehicleDTO result = convertToVehicleDTO(saved);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getVehicleId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created(result, "Thêm xe mới thành công"));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleDTO>> updateVehicle(@PathVariable Integer id, @RequestBody VehicleDTO dto) {
        dto.setVehicleId(id);
        Vehicle updated = mapToEntity(dto);
        Vehicle saved = vehicleService.save(updated);
        return ResponseEntity.ok(ApiResponse.success(convertToVehicleDTO(saved), "Cập nhật thông tin xe thành công"));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteVehicle(@PathVariable Integer id) {
        try {
            Vehicle v = vehicleService.getById(id);
            // Delete all associated images from storage
            for (VehicleImage img : v.getImages()) {
                fileService.deleteFile(img.getImageUrl());
            }
            vehicleService.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa xe thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Không thể xóa xe này vì đang có các lịch đặt xe (Booking) hoặc lịch sử liên quan."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa xe: " + e.getMessage()));
        }
    }

    @PostMapping("/vehicles/{id}/images")
    public ResponseEntity<ApiResponse<Object>> uploadImages(@PathVariable Integer id, @RequestParam("files") MultipartFile[] files) {
        Vehicle vehicle = vehicleService.getById(id);
        try {
            for (MultipartFile file : files) {
                String url = fileService.saveFile(file, "vehicles").getFileUrl();
                VehicleImage img = VehicleImage.builder()
                        .vehicle(vehicle)
                        .imageUrl(url)
                        .isPrimary(vehicle.getImages().isEmpty()) // First image is primary
                        .build();
                vehicleImageRepository.save(img);
            }
            return ResponseEntity.ok(ApiResponse.success(null, "Tải ảnh lên thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi tải ảnh: " + e.getMessage()));
        }
    }

    @DeleteMapping("/vehicles/images/{imageId}")
    public ResponseEntity<ApiResponse<Object>> deleteImage(@PathVariable Integer imageId) {
        try {
            VehicleImage img = vehicleImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));
            fileService.deleteFile(img.getImageUrl());
            vehicleImageRepository.delete(img);
            return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa ảnh!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi xóa ảnh: " + e.getMessage()));
        }
    }

    @PutMapping("/vehicles/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Object>> setPrimaryImage(@PathVariable Integer imageId) {
        try {
            VehicleImage img = vehicleImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));

            // Unset other primary images for this vehicle
            List<VehicleImage> images = vehicleImageRepository.findByVehicle(img.getVehicle());
            for (VehicleImage i : images) {
                i.setIsPrimary(i.getImageId().equals(imageId));
            }
            vehicleImageRepository.saveAll(images);
            return ResponseEntity.ok(ApiResponse.success(null, "Đã đặt làm ảnh chính!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    private Vehicle mapToEntity(VehicleDTO dto) {
        Vehicle v = (dto.getVehicleId() != null) ? vehicleService.getById(dto.getVehicleId()) : new Vehicle();
        v.setLicensePlate(dto.getLicensePlate());

        if (dto.getModelId() != null && dto.getModelId() > 0) {
            v.setModel(vehicleService.getModelById(dto.getModelId()));
        } else if (v.getModel() == null) {
            throw new RuntimeException("Vui lòng chọn mẫu xe hợp lệ");
        }

        v.setManufactureYear(dto.getYear() != null ? dto.getYear()
                : (dto.getManufactureYear() != null ? dto.getManufactureYear() : 2023));
        v.setMileage(dto.getMileage() != null ? dto.getMileage() : 0);
        v.setLastMaintenanceMileage(dto.getLastMaintenanceMileage() != null ? dto.getLastMaintenanceMileage() : 0);

        java.math.BigDecimal price = dto.getDailyRate() != null ? dto.getDailyRate() : dto.getPricePerDay();
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá thuê mỗi ngày phải lớn hơn 0");
        }
        v.setPricePerDay(price);
        
        v.setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : (v.getDepositAmount() != null ? v.getDepositAmount() : java.math.BigDecimal.ZERO));
        

        v.setSeats(dto.getSeats() != null ? dto.getSeats() : 4);
        v.setFuelType(dto.getFuel() != null ? dto.getFuel() : (dto.getFuelType() != null ? dto.getFuelType() : "Xăng"));
        v.setTransmission(dto.getTransmission() != null ? dto.getTransmission() : "Tự động");

        if (dto.getVehicleId() == null) {
            v.setStatus(Vehicle.Status.Available);
        }

        if (dto.getType() != null) {
            v.setType(dto.getType());
        } else if (v.getType() == null) {
            v.setType("Xe Ô Tô");
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
        dto.setDepositAmount(vehicle.getDepositAmount());
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
