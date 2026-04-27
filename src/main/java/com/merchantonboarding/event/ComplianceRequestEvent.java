package com.merchantonboarding.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRequestEvent {

    private String caseId;
    private String documentType;
    private String externalReference;

    private String businessName;
    private String businessType;
    private String registrationNumber;
    private String businessAddress;
    private String directorName;

    // Original filename of the uploaded document being reviewed
    private String documentFileName;

    // Disk path to the uploaded document file
    private String documentFilePath;

    private LocalDateTime requestedAt;
}
