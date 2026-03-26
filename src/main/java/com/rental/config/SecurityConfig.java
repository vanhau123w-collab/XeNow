package com.rental.config;

import com.rental.entity.Customer;
import com.rental.entity.Manager;
import com.rental.repository.CustomerRepository;
import com.rental.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Try Manager first
            var manager = managerRepository.findByUsername(username);
            if (manager.isPresent()) {
                Manager m = manager.get();
                return User.withUsername(m.getUsername())
                        .password(m.getPasswordHash())
                        .roles("MANAGER")
                        .build();
            }
            // Then try Customer
            var customer = customerRepository.findByEmail(username);
            if (customer.isPresent()) {
                Customer c = customer.get();
                return User.withUsername(c.getEmail())
                        .password(c.getPasswordHash())
                        .roles("CUSTOMER")
                        .build();
            }
            throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + username);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/vehicles", "/vehicles/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/register", "/login").permitAll()
                .requestMatchers("/admin/**").hasRole("MANAGER")
                .requestMatchers("/bookings/**", "/my-bookings").hasAnyRole("CUSTOMER", "MANAGER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );
        return http.build();
    }
}
