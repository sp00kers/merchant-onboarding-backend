package com.merchantonboarding.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MerchantCategoryDTO {
    private String id;

    @NotBlank(message = "Code is required")
    @Size(max = 10, message = "Code must be at most 10 characters")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String riskLevel = "low";
    private String status = "active";
    private String createdAt;
    private String updatedAt;
}

