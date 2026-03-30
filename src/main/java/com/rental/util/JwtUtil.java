package com.rental.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${jwt.expiration:3600000}") // 1 giờ
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 ngày
    private Long refreshTokenExpiration;

    public String generateAccessToken(String username, String role, Integer userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("xenow")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenExpiration, ChronoUnit.MILLIS))
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .build();
        
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("xenow")
                .issuedAt(now)
                .expiresAt(now.plus(refreshTokenExpiration, ChronoUnit.MILLIS))
                .subject(username)
                .build();
        
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public String extractUsername(String token) {
        return jwtDecoder.decode(token).getSubject();
    }

    public String extractRole(String token) {
        return jwtDecoder.decode(token).getClaimAsString("role");
    }

    public Integer extractUserId(String token) {
        Object userId = jwtDecoder.decode(token).getClaim("userId");
        if (userId instanceof Number) {
            return ((Number) userId).intValue();
        }
        return null;
    }

    public Boolean validateToken(String token, String username) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return Objects.equals(jwt.getSubject(), username) && 
                   Objects.requireNonNull(jwt.getExpiresAt()).isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }

    public String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi băm Token", e);
        }
    }
}
