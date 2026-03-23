package com.merchantonboarding.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_extractions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExtraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", unique = true)
    private Document document;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "extracted_business_name")
    private String extractedBusinessName;

    @Column(name = "extracted_registration_number")
    private String extractedRegistrationNumber;

    @Column(name = "extracted_director_name")
    private String extractedDirectorName;

    @Column(name = "extracted_director_ic")
    private String extractedDirectorIC;

    @Column(name = "extracted_address", length = 500)
    private String extractedAddress;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "validation_status", length = 50)
    private String validationStatus; // VALIDATED, MISMATCH, PENDING

    @Column(name = "validation_notes", length = 500)
    private String validationNotes;

    @Column(name = "extracted_at")
    private LocalDateTime extractedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @PrePersist
    public void prePersist() {
        this.extractedAt = LocalDateTime.now();
    }
}
