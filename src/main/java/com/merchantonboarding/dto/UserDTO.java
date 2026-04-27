package com.merchantonboarding.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {
    private String id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@bank\\.com$", message = "Email must use @bank.com domain")
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
    private Set<String> customPermissions;
}
