package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.LoginRequest;
import com.rental.entity.Permission;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody User user, HttpServletRequest request) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name("CUSTOMER")
                .description("Khách hàng")
                .build()));

        Set<Role> rolesSet = new HashSet<>();
        rolesSet.add(customerRole);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(rolesSet);
        user.setStatus(User.Status.Active);

        User savedUser = userRepository.save(user);

        List<String> roleNames = List.of("CUSTOMER");
        String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername(), roleNames, savedUser.getUserId());
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
        data.put("user", buildUserMap(savedUser, roleNames));

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

        List<String> roleNames = extractRoleNames(user);
        log.info("[LOGIN] User '{}' logging in with roles: {}", user.getUsername(), roleNames);

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), roleNames, user.getUserId());
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
        data.put("user", buildUserMap(user, roleNames));

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
                    List<String> roleNames = extractRoleNames(user);

                    String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), roleNames, user.getUserId());
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

        List<String> roleNames = extractRoleNames(user);
        Map<String, Object> data = new HashMap<>();
        data.put("user", buildUserMap(user, roleNames));
        data.put("authenticated", true);

        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thông tin người dùng thành công"));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract role names from user, stripping ROLE_ prefix if present.
     */
    private List<String> extractRoleNames(User user) {
                List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                .collect(Collectors.toList());

                if (roleNames.isEmpty()) {
                        List<String> fallbackRoleNames = loadRoleNamesFromJoinTable(user.getUserId());
                        if (!fallbackRoleNames.isEmpty()) {
                                log.warn("[LOGIN] JPA roles empty for user '{}' (id={}), fallback SQL roles={}",
                                                user.getUsername(), user.getUserId(), fallbackRoleNames);
                                roleNames = fallbackRoleNames;
                        }
                }

                return roleNames;
    }

        private List<String> loadRoleNamesFromJoinTable(Integer userId) {
                try {
                        String currentDb = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
                        List<String> roleNames = jdbcTemplate.queryForList(
                                        "SELECT r.name FROM user_role ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?",
                                        String.class,
                                        userId
                        ).stream()
                                        .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                                        .collect(Collectors.toList());

                        log.info("[LOGIN] Direct role lookup in DB '{}' for userId={} => {}",
                                        currentDb, userId, roleNames);
                        return roleNames;
                } catch (Exception e) {
                        log.warn("[LOGIN] Direct role lookup failed for userId={}: {}", userId, e.getMessage());
                        return List.of();
                }
        }

    /**
     * Build user response map. Returns primary role for backward compatibility,
     * plus full roles list and dynamic hasAdminAccess flag.
     */
    private Map<String, Object> buildUserMap(User user, List<String> roles) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("fullName", user.getFullName());
        userMap.put("name", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("address", user.getAddress());
        userMap.put("gender", user.getGender());
        userMap.put("avatar", user.getAvatar());
        userMap.put("dateOfBirth", user.getDateOfBirth());
        userMap.put("status", user.getStatus().toString());
        userMap.put("role", roles.isEmpty() ? "CUSTOMER" : roles.get(0)); // backward compat
        userMap.put("roles", roles); // full list
        userMap.put("hasAdminAccess", checkAdminAccess(roles));
        return userMap;
    }

    /**
     * Dynamically check if user's roles grant access to admin panel.
     * ADMIN role → always true (bypass).
     * Other roles → check if any permission matches /api/admin/** pattern.
     */
    private boolean checkAdminAccess(List<String> roles) {
        org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();
        for (String role : roles) {
            String upperRole = role.toUpperCase();
            // ADMIN always has full access
            if ("ADMIN".equals(upperRole)) {
                return true;
            }
            // Check dynamic permissions from the PermissionAuthorizationManager cache
            String cacheKey = "ROLE_" + upperRole;
            try {
                List<Role> allRoles = roleRepository.findAllWithPermissions();
                for (Role r : allRoles) {
                    if (("ROLE_" + r.getName()).equals(cacheKey) || r.getName().equals(upperRole)) {
                        for (Permission perm : r.getPermissions()) {
                            if (pathMatcher.match("/api/admin/**", perm.getApiPath())) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[AUTH] Failed to check admin access for role {}: {}", role, e.getMessage());
            }
        }
        return false;
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
