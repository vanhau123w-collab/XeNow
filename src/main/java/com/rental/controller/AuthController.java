package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.LoginRequest;
import com.rental.entity.RefreshToken;
import com.rental.entity.Role;
import com.rental.entity.User;
import com.rental.exception.DuplicateResourceException;
import com.rental.exception.ResourceNotFoundException;
import com.rental.repository.RefreshTokenRepository;
import com.rental.repository.RoleRepository;
import com.rental.repository.UserRepository;
import com.rental.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody User user, HttpServletRequest request) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username đã được sử dụng");
        }

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER không tồn tại"));

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(customerRole);
        user.setStatus(User.Status.Active);

        User savedUser = userRepository.save(user);

        String roleStr = "CUSTOMER";
        String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername(), roleStr, savedUser.getUserId());
        String refreshTokenStr = jwtUtil.generateRefreshToken(savedUser.getUsername());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtUtil.hashToken(refreshTokenStr))
                .user(savedUser)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .ipAddress(getClientIp(request))
                .deviceInfo(request.getHeader("User-Agent"))
                .build();
        refreshTokenRepository.save(refreshToken);

        Map<String, Object> data = new HashMap<>();
        data.put("authenticated", true);
        data.put("token", accessToken);
        data.put("refreshToken", refreshTokenStr);
        data.put("user", buildUserMap(savedUser, roleStr));

        ResponseCookie cookie = buildRefreshTokenCookie(refreshTokenStr);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(data, "Đăng ký thành công!"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .or(() -> userRepository.findByEmail(loginRequest.getUsername()))
                .orElse(null);

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Sai tài khoản hoặc mật khẩu"));
        }

        if (user.getStatus() != User.Status.Active) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Tài khoản không có quyền truy cập"));
        }

        String rawRole = (user.getRole() != null)
                ? user.getRole().getRoleName()
                : "CUSTOMER";
        
        // Strip ROLE_ prefix if present to avoid double prefixing with JwtAuthenticationConverter
        String roleStr = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
        
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), roleStr, user.getUserId());
        String refreshTokenStr = jwtUtil.generateRefreshToken(user.getUsername());

        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtUtil.hashToken(refreshTokenStr))
                .user(user)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .ipAddress(getClientIp(request))
                .deviceInfo(request.getHeader("User-Agent"))
                .build();
        refreshTokenRepository.save(refreshToken);

        Map<String, Object> data = new HashMap<>();
        data.put("authenticated", true);
        data.put("token", accessToken);
        data.put("refreshToken", refreshTokenStr);
        data.put("user", buildUserMap(user, roleStr));

        ResponseCookie cookie = buildRefreshTokenCookie(refreshTokenStr);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(data, "Đăng nhập thành công"));
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) Map<String, String> requestBody,
            HttpServletRequest request) {

        String refreshTokenStr = (cookieToken != null && !cookieToken.isEmpty())
                ? cookieToken
                : (requestBody != null ? requestBody.get("refreshToken") : null);

        if (refreshTokenStr == null || refreshTokenStr.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu Refresh Token"));
        }

        String hashedToken = jwtUtil.hashToken(refreshTokenStr);
        return refreshTokenRepository.findByToken(hashedToken)
                .map(tokenEntity -> {
                    if (tokenEntity.getExpiresAt().isBefore(Instant.now()) || tokenEntity.isRevoked()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.<Map<String, Object>>error("Refresh Token đã hết hạn hoặc đã bị thu hồi"));
                    }

                    User user = tokenEntity.getUser();
                    String roleStr = (user.getRole() != null)
                            ? user.getRole().getRoleName()
                            : "CUSTOMER";

                    String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), roleStr, user.getUserId());
                    String newRefreshTokenStr = jwtUtil.generateRefreshToken(user.getUsername());

                    tokenEntity.setRevoked(true);
                    refreshTokenRepository.save(tokenEntity);

                    RefreshToken newRefreshToken = RefreshToken.builder()
                            .token(jwtUtil.hashToken(newRefreshTokenStr))
                            .user(user)
                            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                            .ipAddress(getClientIp(request))
                            .deviceInfo(request.getHeader("User-Agent"))
                            .build();
                    refreshTokenRepository.save(newRefreshToken);

                    ResponseCookie cookie = buildRefreshTokenCookie(newRefreshTokenStr);

                    Map<String, Object> data = new HashMap<>();
                    data.put("token", newAccessToken);
                    data.put("refreshToken", newRefreshTokenStr);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(ApiResponse.success(data, "Làm mới token thành công"));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token không hợp lệ")));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) Map<String, String> requestBody) {

        String refreshTokenStr = (cookieToken != null && !cookieToken.isEmpty())
                ? cookieToken
                : (requestBody != null ? requestBody.get("refreshToken") : null);

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
                .body(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy người dùng"));
        }

        String roleStr = (user.getRole() != null) ? user.getRole().getRoleName()
                : "CUSTOMER";
        Map<String, Object> data = new HashMap<>();
        data.put("user", buildUserMap(user, roleStr));
        data.put("authenticated", true);

        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thông tin người dùng thành công"));
    }

    private Map<String, Object> buildUserMap(User user, String role) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("fullName", user.getFullName());
        userMap.put("name", user.getFullName()); // Added for frontend compatibility
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("address", user.getAddress());
        userMap.put("gender", user.getGender());
        userMap.put("avatar", user.getAvatar());
        userMap.put("dateOfBirth", user.getDateOfBirth());
        userMap.put("status", user.getStatus().toString());
        userMap.put("role", role);
        return userMap;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private ResponseCookie buildRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
    }
}
