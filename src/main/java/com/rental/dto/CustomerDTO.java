package com.rental.dto;

import lombok.Data;

@Data
public class CustomerDTO {
    private Integer userId;
    private Integer customerId; // Added for compatibility
    private String fullName;
    private String name; // Added for compatibility
    private String email;
    private String phone;
    private String identityCard;

}
