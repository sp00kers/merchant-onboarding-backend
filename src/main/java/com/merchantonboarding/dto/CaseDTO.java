package com.merchantonboarding.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaseDTO {
    private Long id;
    
    @NotBlank(message = "Merchant name is required")
    @Size(min = 2, max = 100, message = "Merchant name must be between 2 and 100 characters")
    private String merchantName;
    
    @NotBlank(message = "Business type is required")
    private String businessType;
    
    @NotBlank(message = "Contact person is required")
    private String contactPerson;
    
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;
    
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Address is required")
    @Size(min = 10, message = "Address must be at least 10 characters")
    private String address;
    
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long assignedOfficerId; // Added this field
}
