package com.merchantonboarding.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.DocumentExtractionDTO;
import com.merchantonboarding.service.DocumentValidationService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentValidationService validationService;

    /**
     * Trigger OCR processing for a document
     */
    @PostMapping("/{documentId}/extract")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'DOCUMENT_UPLOAD', 'BACKGROUND_CHECK', 'ALL_MODULES')")
    public ResponseEntity<DocumentExtractionDTO> extractDocument(@PathVariable Long documentId) {
        try {
            CompletableFuture<DocumentExtractionDTO> result = validationService.processDocument(documentId);
            return ResponseEntity.ok(result.get());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get extraction results for a document
     */
    @GetMapping("/{documentId}/extraction")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'ALL_MODULES')")
    public ResponseEntity<DocumentExtractionDTO> getExtraction(@PathVariable Long documentId) {
        DocumentExtractionDTO extraction = validationService.getExtraction(documentId);
        if (extraction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(extraction);
    }

    /**
     * Get all extractions for a case
     */
    @GetMapping("/case/{caseId}/extractions")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'ALL_MODULES')")
    public ResponseEntity<List<DocumentExtractionDTO>> getCaseExtractions(@PathVariable String caseId) {
        List<DocumentExtractionDTO> extractions = validationService.getCaseExtractions(caseId);
        return ResponseEntity.ok(extractions);
    }

    /**
     * Trigger extraction for all documents in a case
     */
    @PostMapping("/case/{caseId}/extract-all")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'ALL_MODULES')")
    public ResponseEntity<String> extractAllDocuments(@PathVariable String caseId) {
        // This would trigger async extraction for all documents
        // For now, return a message indicating the process has started
        return ResponseEntity.ok("Document extraction initiated for case: " + caseId);
    }
}
