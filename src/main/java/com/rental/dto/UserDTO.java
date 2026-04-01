package com.rental.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String status;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
