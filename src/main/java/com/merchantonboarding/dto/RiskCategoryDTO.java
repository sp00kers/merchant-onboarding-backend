package com.merchantonboarding.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RiskCategoryDTO {
    private String id;

    @NotNull(message = "Level is required")
    private Integer level;

    @NotBlank(message = "Name is required")
    private String name;

    private String scoreRange;
    private String description;
    private String actionsRequired;
    private String createdAt;
    private String updatedAt;
}

