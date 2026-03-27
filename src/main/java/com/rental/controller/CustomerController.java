package com.rental.controller;

import com.rental.entity.Customer;
import com.rental.entity.User;
import com.rental.repository.CustomerRepository;
import com.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @GetMapping("/verify-status")
    public ResponseEntity<?> checkVerificationStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        boolean isVerified = customerRepository.findById(user.getUserId()).isPresent();
        
        return ResponseEntity.ok(new VerificationStatusDTO(isVerified, user.getUserId()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyIdentity(@RequestBody Customer customer, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }

        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            // Check if already verified
            if (customerRepository.findById(user.getUserId()).isPresent()) {
                return ResponseEntity.badRequest().body("Tài khoản đã được xác minh");
            }

            // Create customer record
            customer.setUserId(user.getUserId());
            customer.setUser(user);
            customerRepository.save(customer);

            return ResponseEntity.ok("Xác minh danh tính thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Xác minh thất bại: " + e.getMessage());
        }
    }

    // DTO for verification status
    public static class VerificationStatusDTO {
        public boolean verified;
        public Integer userId;

        public VerificationStatusDTO(boolean verified, Integer userId) {
            this.verified = verified;
            this.userId = userId;
        }
    }
}
