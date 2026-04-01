package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.CustomerResponseDTO;
import com.rental.entity.Customer;
import com.rental.entity.DriverLicense;
import com.rental.entity.User;
import com.rental.repository.CustomerRepository;
import com.rental.repository.DriverLicenseRepository;
import com.rental.repository.UserRepository;
import com.rental.service.FptAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final DriverLicenseRepository driverLicenseRepository;
    private final FptAiService fptAiService;
    
    // Configurable paths, simple for now
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/licenses/";

    private void ensureUploadDirExists() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @GetMapping("/verify-status")
    public ResponseEntity<ApiResponse<VerificationStatusDTO>> checkVerificationStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        boolean isVerified = customerRepository.findById(user.getUserId()).isPresent();
        VerificationStatusDTO status = new VerificationStatusDTO(isVerified, user.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(status, "Kiểm tra trạng thái xác minh thành công"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerById(@PathVariable Integer userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }

        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));
        
        CustomerResponseDTO dto = CustomerResponseDTO.builder()
                .userId(customer.getUserId())
                .identityCard(customer.getIdentityCard())
                .identityCardIssueDate(customer.getIdentityCardIssueDate())
                .identityCardExpiry(customer.getIdentityCardExpiry())
                .address(customer.getAddress())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(dto, "Lấy thông tin khách hàng thành công"));
    }

    // OCR endpoints using FPT AI
    @PostMapping("/ocr/cccd")
    public ResponseEntity<?> parseCccdOcr(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, Object> data = fptAiService.scanCccd(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/ocr/driver-license")
    public ResponseEntity<?> parseDriverLicenseOcr(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, Object> data = fptAiService.scanDriverLicense(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // Verify identity (Combining Step 1 and 2 from UI)
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Object>> verifyIdentity(
            @ModelAttribute Customer customer, 
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }

        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            if (customerRepository.findById(user.getUserId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Tài khoản đã được xác minh"));
            }

            // Save Customer (CCCD)
            customer.setUserId(user.getUserId());
            customer.setUser(user);
            customerRepository.save(customer);

            return ResponseEntity.ok(ApiResponse.success(null, "Xác minh danh tính thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Xác minh thất bại: " + e.getMessage()));
        }
    }

    // Get all licenses
    @GetMapping("/licenses")
    public ResponseEntity<ApiResponse<java.util.List<DriverLicense>>> getMyLicenses(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        
        java.util.List<DriverLicense> licenses = driverLicenseRepository.findByCustomer_UserId(user.getUserId());
        // Clean out circular reference
        licenses.forEach(l -> l.setCustomer(null));
        return ResponseEntity.ok(ApiResponse.success(licenses, "Lấy danh sách GPLX thành công"));
    }

    // Direct isolated add license logic
    @PostMapping("/licenses")
    public ResponseEntity<ApiResponse<Object>> uploadNewLicense(
            @RequestParam("licenseNumber") String licenseNumber,
            @RequestParam("licenseClass") String licenseClass,
            @RequestParam("issueDate") String issueDateStr,
            @RequestParam(value = "expiryDate", required = false) String expiryDateStr,
            @RequestParam(value = "image", required = false) MultipartFile file,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }

        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            
            Customer customer = customerRepository.findById(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("Bạn phải xác thực danh tính (CCCD) trước khi thêm GPLX!"));

            String imagePath = null;
            if (file != null && !file.isEmpty()) {
                ensureUploadDirExists();
                String fileName = user.getUserId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File dest = new File(uploadDir + fileName);
                file.transferTo(dest);
                imagePath = "/uploads/licenses/" + fileName;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate issueDt = LocalDate.parse(issueDateStr, formatter);
            LocalDate expiryDt = null;
            if (expiryDateStr != null && !expiryDateStr.isEmpty() && !expiryDateStr.equalsIgnoreCase("Không thời hạn")) {
                expiryDt = LocalDate.parse(expiryDateStr, formatter);
            }

            DriverLicense dl = DriverLicense.builder()
                    .customer(customer)
                    .licenseNumber(licenseNumber)
                    .licenseClass(licenseClass)
                    .issueDate(issueDt)
                    .expiryDate(expiryDt)
                    .imageUrl(imagePath)
                    .build();
            driverLicenseRepository.save(dl);

            return ResponseEntity.ok(ApiResponse.success(null, "Thêm Giấy phép lái xe thành công!"));
        } catch (IOException e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lưu tệp ảnh: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Thêm thất bại: " + e.getMessage()));
        }
    }

    public static class VerificationStatusDTO {
        public boolean verified;
        public Integer userId;

        public VerificationStatusDTO(boolean verified, Integer userId) {
            this.verified = verified;
            this.userId = userId;
        }
    }
}
