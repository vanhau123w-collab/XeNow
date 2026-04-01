package com.rental.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {
    private Integer id;
    private String name;
    private String description;
    private List<PermissionDTO> permissions;
}
