package com.merchantonboarding.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.dto.DocumentExtractionDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.Document;
import com.merchantonboarding.model.DocumentExtraction;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.repository.DocumentExtractionRepository;
import com.merchantonboarding.repository.DocumentRepository;

@Service
@Transactional
public class DocumentValidationService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentExtractionRepository extractionRepository;

    @Autowired
    private OcrService ocrService;

    @Autowired(required = false)
    private NotificationService notificationService;

    /**
     * Process and extract content from a document
     */
    @Async
    public CompletableFuture<DocumentExtractionDTO> processDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        // Check if extraction already exists
        DocumentExtraction existing = extractionRepository.findByDocumentId(documentId).orElse(null);
        if (existing != null && existing.getValidationStatus() != null) {
            return CompletableFuture.completedFuture(convertToDTO(existing));
        }

        // Create or update extraction record
        DocumentExtraction extraction = existing != null ? existing : new DocumentExtraction();
        extraction.setDocument(document);
        extraction.setValidationStatus("PROCESSING");
        extraction = extractionRepository.save(extraction);

        try {
            // Get file and perform OCR
            File file = new File(document.getFilePath());
            OcrService.OcrResult ocrResult = ocrService.extractText(file, document.getType()).get();

            if (ocrResult.isSuccess()) {
                // Save extraction results
                extraction.setRawText(ocrResult.getRawText());
                extraction.setExtractedBusinessName(ocrResult.getExtractedBusinessName());
                extraction.setExtractedRegistrationNumber(ocrResult.getExtractedRegistrationNumber());
                extraction.setExtractedDirectorName(ocrResult.getExtractedDirectorName());
                extraction.setExtractedDirectorIC(ocrResult.getExtractedDirectorIC());
                extraction.setExtractedAddress(ocrResult.getExtractedAddress());
                extraction.setConfidenceScore(ocrResult.getConfidenceScore());

                // Validate against case data
                validateExtraction(extraction, document.getOnboardingCase());
            } else {
                extraction.setValidationStatus("FAILED");
                extraction.setValidationNotes("OCR processing failed: " + ocrResult.getErrorMessage());
            }

            extraction = extractionRepository.save(extraction);

            // Send notification
            if (notificationService != null && document.getOnboardingCase() != null) {
                String caseId = document.getOnboardingCase().getCaseId();
                // Notification logic can be added here
            }

            return CompletableFuture.completedFuture(convertToDTO(extraction));

        } catch (Exception e) {
            extraction.setValidationStatus("FAILED");
            extraction.setValidationNotes("Error processing document: " + e.getMessage());
            extractionRepository.save(extraction);
            return CompletableFuture.completedFuture(convertToDTO(extraction));
        }
    }

    /**
     * Get extraction results for a document
     */
    public DocumentExtractionDTO getExtraction(Long documentId) {
        return extractionRepository.findByDocumentId(documentId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Get all extractions for a case
     */
    public List<DocumentExtractionDTO> getCaseExtractions(String caseId) {
        return extractionRepository.findByCaseId(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validate extracted data against case data
     */
    private void validateExtraction(DocumentExtraction extraction, OnboardingCase caseData) {
        if (caseData == null) {
            extraction.setValidationStatus("PENDING");
            extraction.setValidationNotes("Case data not available for validation");
            return;
        }

        List<String> mismatches = new ArrayList<>();
        int matchCount = 0;
        int totalFields = 0;

        // Check business name
        if (extraction.getExtractedBusinessName() != null) {
            totalFields++;
            if (caseData.getBusinessName() != null &&
                    containsIgnoreCase(extraction.getExtractedBusinessName(), caseData.getBusinessName())) {
                matchCount++;
            } else {
                mismatches.add("Business name mismatch");
            }
        }

        // Check registration number
        if (extraction.getExtractedRegistrationNumber() != null) {
            totalFields++;
            if (caseData.getRegistrationNumber() != null &&
                    normalizeString(extraction.getExtractedRegistrationNumber()).equals(
                            normalizeString(caseData.getRegistrationNumber()))) {
                matchCount++;
            } else {
                mismatches.add("Registration number mismatch");
            }
        }

        // Check director name
        if (extraction.getExtractedDirectorName() != null) {
            totalFields++;
            if (caseData.getDirectorName() != null &&
                    containsIgnoreCase(extraction.getExtractedDirectorName(), caseData.getDirectorName())) {
                matchCount++;
            } else {
                mismatches.add("Director name mismatch");
            }
        }

        // Check director IC
        if (extraction.getExtractedDirectorIC() != null) {
            totalFields++;
            if (caseData.getDirectorIC() != null &&
                    normalizeIC(extraction.getExtractedDirectorIC()).equals(
                            normalizeIC(caseData.getDirectorIC()))) {
                matchCount++;
            } else {
                mismatches.add("Director IC mismatch");
            }
        }

        // Determine validation status
        if (totalFields == 0) {
            extraction.setValidationStatus("PENDING");
            extraction.setValidationNotes("No fields extracted for validation");
        } else if (mismatches.isEmpty()) {
            extraction.setValidationStatus("VALIDATED");
            extraction.setValidationNotes("All extracted fields match case data");
        } else if (matchCount > 0) {
            extraction.setValidationStatus("PARTIAL_MATCH");
            extraction.setValidationNotes(String.format("%d/%d fields match. Issues: %s",
                    matchCount, totalFields, String.join(", ", mismatches)));
        } else {
            extraction.setValidationStatus("MISMATCH");
            extraction.setValidationNotes("Extracted data does not match: " + String.join(", ", mismatches));
        }

        extraction.setValidatedAt(LocalDateTime.now());
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) return false;
        String normalizedSource = source.toLowerCase().replaceAll("[^a-z0-9]", "");
        String normalizedTarget = target.toLowerCase().replaceAll("[^a-z0-9]", "");
        return normalizedSource.contains(normalizedTarget) || normalizedTarget.contains(normalizedSource);
    }

    private String normalizeString(String str) {
        if (str == null) return "";
        return str.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private String normalizeIC(String ic) {
        if (ic == null) return "";
        return ic.replaceAll("[^0-9]", "");
    }

    private DocumentExtractionDTO convertToDTO(DocumentExtraction extraction) {
        DocumentExtractionDTO dto = new DocumentExtractionDTO();
        dto.setId(extraction.getId());
        dto.setDocumentId(extraction.getDocument() != null ? extraction.getDocument().getId() : null);
        dto.setDocumentName(extraction.getDocument() != null ? extraction.getDocument().getName() : null);
        dto.setDocumentType(extraction.getDocument() != null ? extraction.getDocument().getType() : null);
        dto.setRawText(extraction.getRawText());
        dto.setExtractedBusinessName(extraction.getExtractedBusinessName());
        dto.setExtractedRegistrationNumber(extraction.getExtractedRegistrationNumber());
        dto.setExtractedDirectorName(extraction.getExtractedDirectorName());
        dto.setExtractedDirectorIC(extraction.getExtractedDirectorIC());
        dto.setExtractedAddress(extraction.getExtractedAddress());
        dto.setConfidenceScore(extraction.getConfidenceScore());
        dto.setValidationStatus(extraction.getValidationStatus());
        dto.setValidationNotes(extraction.getValidationNotes());
        dto.setExtractedAt(extraction.getExtractedAt());
        dto.setValidatedAt(extraction.getValidatedAt());
        return dto;
    }
}
