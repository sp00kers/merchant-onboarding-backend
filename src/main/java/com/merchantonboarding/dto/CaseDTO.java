package com.merchantonboarding.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CaseDTO {
    private String caseId;

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType;
    
    @NotBlank(message = "Registration number is required")
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

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String directorPhone;

    @Email(message = "Email should be valid")
    private String directorEmail;

    private String status;
    private String createdDate;
    private String assignedTo;
    private String priority;
    private String lastUpdated;

    private List<DocumentDTO> documents;
    private List<CaseHistoryDTO> history;

    @Data
    public static class DocumentDTO {
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
