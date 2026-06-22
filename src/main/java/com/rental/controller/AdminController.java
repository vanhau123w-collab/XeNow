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
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * Controller dành cho Quản trị viên (Admin).
 * Chứa các API quản lý toàn bộ hệ thống: Thống kê, Duyệt đơn, Quản lý Xe, Người
 * dùng, Chi nhánh và hãng xe.
 */
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
    private final com.rental.repository.RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * API hỗ trợ khôi phục mật khẩu cho một User cụ thể (Dùng trong trường hợp khẩn
     * cấp/demo).
     */
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

            return ResponseEntity
                    .ok("Mật khẩu của " + user.getUsername() + " (ID 3) đã được đổi thành: 123456.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Lấy dữ liệu tổng quan cho trang Dashboard (Thống kê số lượng xe, đơn hàng
     * đang chờ...).
     */
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

    /**
     * Xuất dữ liệu báo cáo tổng hợp dưới dạng danh sách Bookings và Vehicles.
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> reports() {
        List<BookingDTO> bookings = bookingService.getAllBookings().stream()
                .map(this::convertToBookingDTO)
                .collect(Collectors.toList());
        List<VehicleDTO> vehicles = vehicleService.getAllVehicles().stream()
                .map(this::convertToVehicleDTO)
                .collect(Collectors.toList());

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("bookings", bookings);
        data.put("vehicles", vehicles);
        return ResponseEntity.ok(ApiResponse.success(data, "Tải dữ liệu báo cáo thành công"));
    }

    /**
     * Lấy danh sách tất cả các đơn đặt xe (có phân trang).
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Page<BookingDTO>>> allBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by("bookingId").ascending()
                : Sort.by("bookingId").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BookingDTO> bookings = bookingService.getAllBookings(pageable)
                .map(this::convertToBookingDTO);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Lấy danh sách đặt xe thành công"));
    }

    /**
     * Cập nhật trạng thái của đơn đặt xe (Ví dụ: Từ Pending sang Confirmed hoặc
     * Completed).
     * Bao gồm cập nhật số km khi trả xe và ghi chú từ Admin.
     */
    @PostMapping("/bookings/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateBookingStatus(@PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) Integer mileage,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String returnPaymentMethod) {
        try {
            bookingService.updateStatus(id, Booking.Status.valueOf(status), mileage, note, returnPaymentMethod);
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách toàn bộ phương tiện (có phân trang).
     */
    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<Page<VehicleDTO>>> allVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by("pricePerDay").descending()
                : Sort.by("pricePerDay").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VehicleDTO> vehicles = vehicleService.getAllVehicles(pageable)
                .map(this::convertToVehicleDTO);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Lấy danh sách xe thành công"));
    }

    /**
     * Lấy thông tin chi tiết của một xe theo ID.
     */
    @GetMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleDTO>> getVehicleById(@PathVariable Integer id) {
        Vehicle vehicle = vehicleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToVehicleDTO(vehicle), "Lấy thông tin xe thành công"));
    }

    /**
     * Thay đổi trạng thái của xe (Ví dụ: Chuyển sang Maintenance khi cần bảo trì).
     */
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

    /**
     * Xác nhận hoàn tất bảo trì xe để đưa xe trở lại trạng thái Sẵn sàng
     * (Available).
     */
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

    /**
     * Quản lý danh sách Khách hàng.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> allCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by("userId").descending()
                    : Sort.by("userId").ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<CustomerDTO> customers = customerService.getAll(pageable)
                    .map(this::convertToCustomerDTO);
            return ResponseEntity.ok(ApiResponse.success(customers, "Lấy danh sách khách hàng thành công"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách khách hàng: " + e.getMessage()));
        }
    }

    /**
     * Quản lý toàn bộ người dùng trong hệ thống (nhân viên, khách hàng, quản trị
     * viên).
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> allUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            @RequestParam(required = false) String keyword) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].equals("id") ? "userId" : sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<User> userPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userRepository.search(keyword.trim(), pageable);
        } else {
            userPage = userRepository.findAllWithRoles(pageable);
        }

        Page<UserDTO> dtoPage = userPage.map(this::convertToUserDTO);
        return ResponseEntity.ok(ApiResponse.success(dtoPage, "Lấy danh sách người dùng thành công"));
    }

    /**
     * Cập nhật vai trò (Role) cho người dùng (Phân quyền).
     */
    @PostMapping("/users/{id}/roles")
    public ResponseEntity<ApiResponse<Object>> updateUserRoles(@PathVariable Integer id,
            @RequestBody Set<String> roleNames) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Set<Role> roles = roleNames.stream()
                    .map(name -> {
                        String searchName = name.trim().toUpperCase();
                        return roleRepository.findByName(searchName)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò: " + searchName));
                    })
                    .collect(Collectors.toSet());

            user.setRoles(roles);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật vai trò người dùng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Quản lý các chi nhánh (Locations) của hệ thống cho thuê xe.
     */
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<Page<LocationDTO>>> allLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by("locationId").descending()
                : Sort.by("locationId").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<LocationDTO> locations = locationRepository.findAll(pageable)
                .map(this::convertToLocationDTO);
        return ResponseEntity.ok(ApiResponse.success(locations, "Lấy danh sách chi nhánh thành công"));
    }

    /**
     * Thêm mới một chi nhánh.
     */
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
        return ResponseEntity.ok(ApiResponse.success(result, "Thêm chi nhánh mới thành công"));
    }

    /**
     * Xóa chi nhánh (Chỉ xóa được nếu không còn xe nào thuộc chi nhánh đó).
     */
    @DeleteMapping("/locations/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteLocation(@PathVariable Integer id) {
        try {
            locationRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa chi nhánh thành công!"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Không thể xóa chi nhánh vì còn dữ liệu xe liên quan."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa: " + e.getMessage()));
        }
    }

    /**
     * Thêm mới xe vào danh mục quản lý.
     */
    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse<VehicleDTO>> createVehicle(@RequestBody VehicleDTO dto) {
        Vehicle vehicle = mapToEntity(dto);
        Vehicle saved = vehicleService.save(vehicle);
        VehicleDTO result = convertToVehicleDTO(saved);
        return ResponseEntity.ok(ApiResponse.success(result, "Thêm xe mới thành công"));
    }

    /**
     * Lưu trữ ảnh của xe lên server thông qua FileService.
     */
    @PostMapping("/vehicles/{id}/images")
    public ResponseEntity<ApiResponse<Object>> uploadImages(@PathVariable Integer id,
            @RequestParam("files") MultipartFile[] files) {
        Vehicle vehicle = vehicleService.getById(id);
        try {
            for (MultipartFile file : files) {
                String url = fileService.saveFile(file, "vehicles").getFileUrl();
                VehicleImage img = VehicleImage.builder()
                        .vehicle(vehicle)
                        .imageUrl(url)
                        .isPrimary(vehicle.getImages().isEmpty())
                        .build();
                vehicleImageRepository.save(img);
            }
            return ResponseEntity.ok(ApiResponse.success(null, "Tải ảnh lên thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi tải ảnh: " + e.getMessage()));
        }
    }

    // --- CÁC PHƯƠNG THỨC TRỢ GIÚP (HELPER METHODS) ---

    private BookingDTO convertToBookingDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setVehicleId(booking.getVehicle().getVehicleId());
        dto.setVehicleModel(booking.getVehicle().getFullName());
        dto.setCustomerName(booking.getCustomer().getName());
        dto.setCustomerPhone(booking.getCustomer().getPhone());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setStatus(booking.getStatus());
        dto.setPickupLocationName(booking.getPickupAddress());
        dto.setReturnLocationName(booking.getReturnAddress());
        return dto;
    }

    private VehicleDTO convertToVehicleDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getVehicleId());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setName(vehicle.getFullName());
        dto.setPricePerDay(vehicle.getPricePerDay());
        dto.setStatus(vehicle.getStatus());
        if (vehicle.getCurrentLocation() != null) {
            dto.setLocationName(vehicle.getCurrentLocation().getBranchName());
        }
        return dto;
    }

    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }

    private CustomerDTO convertToCustomerDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setUserId(customer.getUserId());
        dto.setCustomerId(customer.getUserId());
        dto.setFullName(customer.getName());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setIdentityCard(customer.getIdentityCard());
        return dto;
    }


    private LocationDTO convertToLocationDTO(com.rental.entity.Location location) {
        LocationDTO dto = new LocationDTO();
        dto.setLocationId(location.getLocationId());
        dto.setBranchName(location.getBranchName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setPhone(location.getPhone());
        dto.setVehicleCount(vehicleRepository.countByCurrentLocationLocationId(location.getLocationId()));
        return dto;
    }

    private Vehicle mapToEntity(VehicleDTO dto) {
        Vehicle v = (dto.getVehicleId() != null) ? vehicleService.getById(dto.getVehicleId()) : new Vehicle();
        v.setLicensePlate(dto.getLicensePlate());
        if (dto.getModelId() != null)
            v.setModel(vehicleService.getModelById(dto.getModelId()));
        v.setManufactureYear(dto.getYear() != null ? dto.getYear() : 2023);
        v.setPricePerDay(dto.getDailyRate() != null ? dto.getDailyRate() : BigDecimal.ZERO);
        v.setSeats(dto.getSeats() != null ? dto.getSeats() : 4);
        v.setStatus(Vehicle.Status.Available);
        if (dto.getLocationId() != null)
            v.setCurrentLocation(vehicleService.getLocationById(dto.getLocationId()));
        return v;
    }
}
