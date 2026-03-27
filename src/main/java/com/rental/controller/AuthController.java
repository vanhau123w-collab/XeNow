package com.rental.controller;

import com.rental.dto.AuthResponseDTO;
import com.rental.entity.Role;
import com.rental.entity.User;
import com.rental.repository.RoleRepository;
import com.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // Check if email or username already exists
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email đã được sử dụng");
            }
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username đã được sử dụng");
            }
            
            // Get CUSTOMER role
            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));
            
            // Setup user
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRole(customerRole);
            user.setStatus(User.Status.Active);
            
            // Save user only
            userRepository.save(user);
            
            return ResponseEntity.ok("Đăng ký thành công! Vui lòng đăng nhập và xác thực danh tính.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Đăng ký thất bại: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }
        return ResponseEntity.ok(new AuthResponseDTO(
            "Đã đăng nhập",
            authentication.getName(),
            authentication.getAuthorities().toString(),
            true
        ));
    }
}
