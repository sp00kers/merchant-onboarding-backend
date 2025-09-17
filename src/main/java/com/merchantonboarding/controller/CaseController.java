package com.merchantonboarding.controller;

import com.merchantonboarding.dto.CaseDTO;
import com.merchantonboarding.model.OnboardingCase.CaseStatus;
import com.merchantonboarding.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "http://localhost:4200")
public class CaseController {
    
    @Autowired
    private CaseService caseService;
    
    /**
     * Get all cases with pagination and filtering
     * Supports query parameters for page, size, and status
     */
    @GetMapping
    public ResponseEntity<Page<CaseDTO>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        Page<CaseDTO> cases = caseService.getAllCases(page, size, status);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Create new merchant onboarding case
     */
    @PostMapping
    public ResponseEntity<CaseDTO> createCase(@Valid @RequestBody CaseDTO caseDTO) {
        CaseDTO createdCase = caseService.createCase(caseDTO);
        return ResponseEntity.ok(createdCase);
    }
    
    /**
     * Get case by ID with path parameter
     */
    @GetMapping("/{id}")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable Long id) {
        CaseDTO caseDTO = caseService.getCaseById(id);
        return ResponseEntity.ok(caseDTO);
    }
    
    /**
     * Update existing case
     */
    @PutMapping("/{id}")
    public ResponseEntity<CaseDTO> updateCase(@PathVariable Long id, 
                                              @Valid @RequestBody CaseDTO caseDTO) {
        CaseDTO updatedCase = caseService.updateCase(id, caseDTO);
        return ResponseEntity.ok(updatedCase);
    }
    
    /**
     * Delete case (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(@PathVariable Long id) {
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get cases by assigned officer - custom derived query
     */
    @GetMapping("/by-officer/{officerId}")
    public ResponseEntity<List<CaseDTO>> getCasesByOfficer(@PathVariable Long officerId) {
        List<CaseDTO> cases = caseService.getCasesByOfficer(officerId);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Search cases - JPQL query implementation
     */
    @GetMapping("/search")
    public ResponseEntity<List<CaseDTO>> searchCases(@RequestParam String keyword) {
        List<CaseDTO> cases = caseService.searchCases(keyword);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Get case statistics - native SQL query
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getCaseStatistics() {
        Map<String, Long> statistics = caseService.getCaseStatistics();
        return ResponseEntity.ok(statistics);
    }
}
