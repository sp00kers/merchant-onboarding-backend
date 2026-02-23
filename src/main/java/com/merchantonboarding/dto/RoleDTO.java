package com.merchantonboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class RoleDTO {
    private String id;

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;

    private Set<String> permissions;

    @JsonProperty("isActive")
    private boolean isActive = true;

    private String createdAt;
    private String updatedAt;
}

