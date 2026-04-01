package com.rental.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PermissionDTO {
    private Integer id;
    private String name;
    private String apiPath;
    private String method;
    private String module;
}
