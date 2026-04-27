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
public class ComplianceResponseEvent {

    private String caseId;
    private String documentType;
    private String externalReference;

    private String status; // PASSED or FAILED
    private String reason;

    private LocalDateTime completedAt;
}
