package com.rental.controller;

import com.rental.dto.AuthResponseDTO;
import com.rental.dto.LoginRequest;
import com.rental.entity.Role;
import com.rental.entity.User;
import com.rental.repository.RoleRepository;
import com.rental.repository.UserRepository;
import com.rental.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("Login attempt: " + loginRequest.getUsername());
            
            // Find user by username or email
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .or(() -> userRepository.findByEmail(loginRequest.getUsername()))
                    .orElse(null);
            
            if (user == null) {
                System.out.println("User not found: " + loginRequest.getUsername());
                return ResponseEntity.ok(new AuthResponseDTO(
                    "Sai tài khoản hoặc mật khẩu",
                    null,
                    null,
                    false
                ));
            }
            
            System.out.println("User found: " + user.getUsername());
            
            boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            System.out.println("Password matches: " + passwordMatches);
            
            if (!passwordMatches) {
                return ResponseEntity.ok(new AuthResponseDTO(
                    "Sai tài khoản hoặc mật khẩu",
                    null,
                    null,
                    false
                ));
            }
            
            // Check if user is active
            if (user.getStatus() != User.Status.Active) {
                return ResponseEntity.ok(new AuthResponseDTO(
                    "Tài khoản đã bị khóa",
                    null,
                    null,
                    false
                ));
            }
            
            // Generate JWT token
            String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().getRoleName(),
                user.getUserId()
            );
            
            System.out.println("Login successful for: " + user.getUsername());
            
            // Create response map
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("username", user.getUsername());
            userMap.put("fullName", user.getFullName());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("role", user.getRole().getRoleName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đăng nhập thành công");
            response.put("authenticated", true);
            response.put("token", token);
            response.put("user", userMap);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new AuthResponseDTO(
                "Đăng nhập thất bại: " + e.getMessage(),
                null,
                null,
                false
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }
        
        // Get user from database
        String username = authentication.getName();
        var user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(404).body("Không tìm thấy thông tin người dùng");
        }
        
        // Create user map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("fullName", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("dateOfBirth", user.getDateOfBirth());
        userMap.put("role", user.getRole().getRoleName());
        userMap.put("status", user.getStatus().toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", userMap);
        response.put("authenticated", true);
        
        return ResponseEntity.ok(response);
    }
}
