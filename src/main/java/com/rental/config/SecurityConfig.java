package com.rental.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rental.repository.UserRepository;
import com.rental.entity.Role;
import com.rental.security.PermissionAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.http.HttpMethod;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final PermissionAuthorizationManager permissionAuthorizationManager;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no auth required
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**",
                                "/uploads/**")
                        .permitAll()
                        .requestMatchers("/api/auth/**", "/api/admin/rescue-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**", "/api/brands/**", "/api/models/**",
                                "/api/branches/**", "/api/locations/**")
                        .permitAll()

                        // All other requests → check dynamically via PermissionAuthorizationManager
                        .anyRequest().access(permissionAuthorizationManager))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter()
                                    .write("{\"message\":\"Bạn cần đăng nhập để thực hiện hành động này\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter()
                                    .write("{\"message\":\"Bạn không có quyền truy cập tài nguyên này\"}");
                        }));

        return http.build();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:uploads/");
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            var user = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

            String[] roles = user.getRoles().stream()
                    .map(Role::getName)
                    .toArray(String[]::new);

            if (roles.length == 0) {
                roles = new String[]{"CUSTOMER"};
            }

            return User.withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(roles)
                    .build();
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var log = org.slf4j.LoggerFactory.getLogger("JwtRoleConverter");

            // Log all claims for debugging
            log.info("[JWT-CONVERTER] Token subject: {}", jwt.getSubject());
            log.info("[JWT-CONVERTER] All claims: {}", jwt.getClaims().keySet());

            Object roleClaim = jwt.getClaim("role");
            log.info("[JWT-CONVERTER] 'role' claim raw value: {} (type: {})",
                    roleClaim, roleClaim != null ? roleClaim.getClass().getName() : "null");

            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
                    new java.util.ArrayList<>();

            if (roleClaim instanceof java.util.Collection<?> roles) {
                for (Object r : roles) {
                    String authority = "ROLE_" + r.toString();
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
                    log.info("[JWT-CONVERTER] Added authority: {}", authority);
                }
            } else if (roleClaim instanceof String roleStr) {
                String authority = "ROLE_" + roleStr;
                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
                log.info("[JWT-CONVERTER] Added authority from string: {}", authority);
            } else {
                log.warn("[JWT-CONVERTER] No 'role' claim found or unsupported type! Token may be stale.");
            }

            log.info("[JWT-CONVERTER] Final authorities: {}", authorities);
            return authorities;
        });
        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Cache-Control"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
