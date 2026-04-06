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
public class VerificationResponseEvent {

    private String caseId;
    private String verificationType;
    private String externalReference;

    private String status; // COMPLETED or FAILED
    private int confidenceScore;
    private String responseData; // JSON string
    private String riskIndicators; // JSON array string
    private String notes;

    private LocalDateTime completedAt;
}
