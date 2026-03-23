package com.merchantonboarding.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DocumentExtractionDTO {
    private Long id;
    private Long documentId;
    private String documentName;
    private String documentType;
    private String rawText;
    private String extractedBusinessName;
    private String extractedRegistrationNumber;
    private String extractedDirectorName;
    private String extractedDirectorIC;
    private String extractedAddress;
    private Integer confidenceScore;
    private String validationStatus;
    private String validationNotes;
    private LocalDateTime extractedAt;
    private LocalDateTime validatedAt;

    @Data
    public static class ExtractionSummary {
        private String caseId;
        private int totalDocuments;
        private int processedCount;
        private int validatedCount;
        private int mismatchCount;
        private Integer averageConfidenceScore;
        private String overallStatus;
    }
}
