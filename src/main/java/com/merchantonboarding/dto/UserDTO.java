package com.merchantonboarding.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class UserDTO {
    private String id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    private String roleId;

    private String department;
    private String phone;
    private String status = "active";
    private String lastLogin;
    private String notes;
    private String createdAt;
    private String updatedAt;

    // For login compatibility
    private String username;

    // Role details (for response)
    private RoleDTO role;
    private Set<String> permissions;
}
