package com.merchantonboarding.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CaseDTO {
    private String caseId;

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType;
    
    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Registration Number must have 12 numbers.")
    private String registrationNumber;

    @NotBlank(message = "Merchant category is required")
    private String merchantCategory;

    @NotBlank(message = "Business address is required")
    @Size(min = 10, message = "Business address must be at least 10 characters")
    private String businessAddress;

    @NotBlank(message = "Director name is required")
    private String directorName;

    @NotBlank(message = "Director IC is required")
    private String directorIC;

    @Pattern(regexp = "^\\+?[0-9]{8,11}$", message = "The phone number's length should be between 8 to 11.")
    private String directorPhone;

    @Email(message = "Email should be valid")
    private String directorEmail;

    private String status;
    private String rejectedAtStage;
    private String createdDate;
    private String assignedTo;
    private String lastUpdated;

    private List<DocumentDTO> documents;
    private List<CaseHistoryDTO> history;

    @Data
    public static class DocumentDTO {
        private Long id;
        private String name;
        private String type;
        private String uploadedAt;
    }

    @Data
    public static class CaseHistoryDTO {
        private String time;
        private String action;
    }
}
