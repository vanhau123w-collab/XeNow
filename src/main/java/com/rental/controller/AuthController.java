package com.rental.controller;

import com.rental.dto.AuthResponseDTO;
import com.rental.dto.LoginRequest;
import com.rental.entity.RefreshToken;
import com.rental.entity.Role;
import com.rental.entity.User;
import com.rental.repository.RefreshTokenRepository;
import com.rental.repository.RoleRepository;
import com.rental.repository.UserRepository;
import com.rental.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CookieValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email đã được sử dụng");
            }
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username đã được sử dụng");
            }
            
            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));
            
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(List.of(customerRole));
            user.setStatus(User.Status.Active);
            
            User savedUser = userRepository.save(user);

            // Generate Tokens
            String roleStr = "CUSTOMER";
            String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername(), roleStr, savedUser.getUserId());
            String refreshTokenStr = jwtUtil.generateRefreshToken(savedUser.getUsername());

            // Save Refresh Token (Hashed)
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(jwtUtil.hashToken(refreshTokenStr));
            refreshToken.setUser(savedUser);
            refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            refreshTokenRepository.save(refreshToken);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", savedUser.getUserId());
            userMap.put("username", savedUser.getUsername());
            userMap.put("fullName", savedUser.getFullName());
            userMap.put("email", savedUser.getEmail());
            userMap.put("phone", savedUser.getPhone());
            userMap.put("address", savedUser.getAddress());
            userMap.put("gender", savedUser.getGender());
            userMap.put("avatar", savedUser.getAvatar());
            userMap.put("dateOfBirth", savedUser.getDateOfBirth());
            userMap.put("status", savedUser.getStatus().toString());
            userMap.put("role", roleStr);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đăng ký thành công!");
            response.put("authenticated", true);
            response.put("accessToken", accessToken);
            // refreshTokenStr is now in cookie
            response.put("user", userMap);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshTokenStr)
                    .httpOnly(true)
                    .secure(false) // true in production
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60) // 7 days
                    .sameSite("Lax")
                    .build();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Đăng ký thất bại: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .or(() -> userRepository.findByEmail(loginRequest.getUsername()))
                    .orElse(null);
            
            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.ok(new AuthResponseDTO("Sai tài khoản hoặc mật khẩu", null, null, false));
            }
            
            if (user.getStatus() != User.Status.Active) {
                return ResponseEntity.ok(new AuthResponseDTO("Tài khoản đã bị khóa", null, null, false));
            }
            
            String roleStr = (user.getRoles() != null && !user.getRoles().isEmpty()) ? user.getRoles().get(0).getRoleName() : "CUSTOMER";
            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), roleStr, user.getUserId());
            String refreshTokenStr = jwtUtil.generateRefreshToken(user.getUsername());

            // Manage Refresh Token (Hashed)
            refreshTokenRepository.deleteByUser(user);
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(jwtUtil.hashToken(refreshTokenStr));
            refreshToken.setUser(user);
            refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            refreshTokenRepository.save(refreshToken);
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("username", user.getUsername());
            userMap.put("fullName", user.getFullName());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("address", user.getAddress());
            userMap.put("gender", user.getGender());
            userMap.put("avatar", user.getAvatar());
            userMap.put("dateOfBirth", user.getDateOfBirth());
            userMap.put("status", user.getStatus().toString());
            userMap.put("role", roleStr);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đăng nhập thành công");
            response.put("authenticated", true);
            response.put("accessToken", accessToken);
            // refreshTokenStr is now in cookie
            response.put("user", userMap);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshTokenStr)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) Map<String, String> request,
            @CookieValue(name = "refreshToken", required = false) String cookieToken) {
        
        String refreshTokenStr = (request != null && request.containsKey("refreshToken")) 
                ? request.get("refreshToken") : cookieToken;
        
        if (refreshTokenStr == null || refreshTokenStr.isEmpty()) {
            return ResponseEntity.badRequest().body("Thiếu Refresh Token");
        }

        String hashedToken = jwtUtil.hashToken(refreshTokenStr);
        return refreshTokenRepository.findByToken(hashedToken)
                .map(tokenEntity -> {
                    // Check if expired or revoked
                    if (tokenEntity.getExpiresAt().isBefore(Instant.now()) || tokenEntity.isRevoked()) {
                        return ResponseEntity.status(401).body("Refresh Token đã hết hạn hoặc đã bị thu hồi");
                    }

                    User user = tokenEntity.getUser();
                    String roleStr = (user.getRoles() != null && !user.getRoles().isEmpty()) 
                        ? user.getRoles().get(0).getRoleName() : "CUSTOMER";

                    // Generate new pair
                    String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), roleStr, user.getUserId());
                    String newRefreshTokenStr = jwtUtil.generateRefreshToken(user.getUsername());

                    // Rotate token (Revoke old, save new)
                    tokenEntity.setRevoked(true);
                    refreshTokenRepository.save(tokenEntity);
                    
                    RefreshToken newRefreshToken = new RefreshToken();
                    newRefreshToken.setToken(jwtUtil.hashToken(newRefreshTokenStr));
                    newRefreshToken.setUser(user);
                    newRefreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
                    refreshTokenRepository.save(newRefreshToken);

                    ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshTokenStr)
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(7 * 24 * 60 * 60)
                            .sameSite("Lax")
                            .build();

                    Map<String, Object> response = new HashMap<>();
                    response.put("accessToken", newAccessToken);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(response);
                })
                .orElse(ResponseEntity.status(401).body("Refresh Token không hợp lệ"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) Map<String, String> request,
            @CookieValue(name = "refreshToken", required = false) String cookieToken) {
        
        String refreshTokenStr = (request != null && request.containsKey("refreshToken")) 
                ? request.get("refreshToken") : cookieToken;

        if (refreshTokenStr != null && !refreshTokenStr.isEmpty()) {
            String hashedToken = jwtUtil.hashToken(refreshTokenStr);
            refreshTokenRepository.findByToken(hashedToken)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        }

        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(404).body("Không tìm thấy người dùng");
        }
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("fullName", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("address", user.getAddress());
        userMap.put("gender", user.getGender());
        userMap.put("avatar", user.getAvatar());
        userMap.put("dateOfBirth", user.getDateOfBirth());
        userMap.put("status", user.getStatus().toString());
        userMap.put("role", (user.getRoles() != null && !user.getRoles().isEmpty()) ? user.getRoles().get(0).getRoleName() : "CUSTOMER");
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", userMap);
        response.put("authenticated", true);
        
        return ResponseEntity.ok(response);
    }
}
