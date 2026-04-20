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
public class VerificationRequestEvent {

    private String caseId;
    private String verificationType;
    private String externalReference;

    // Business details for verification
    private String businessName;
    private String businessType;
    private String registrationNumber;
    private String businessAddress;

    // Director details
    private String directorName;
    private String directorIC;
    private String directorPhone;
    private String directorEmail;

    private Double ownershipPercentage;

    private LocalDateTime requestedAt;
}
