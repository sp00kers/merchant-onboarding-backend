package com.merchantonboarding.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PermissionDTO {
    private String id;

    @NotBlank(message = "Permission name is required")
    private String name;

    private String description;
    private String category;
    private boolean isActive = true;
    private String createdAt;
    private String updatedAt;
}

