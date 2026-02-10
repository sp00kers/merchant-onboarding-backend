package com.merchantonboarding.controller;

import com.merchantonboarding.dto.CaseDTO;
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
     */
    @GetMapping
    public ResponseEntity<List<CaseDTO>> getAllCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        List<CaseDTO> cases = caseService.filterCases(status, search);
        return ResponseEntity.ok(cases);
    }

    /**
     * Get all cases with pagination
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<CaseDTO>> getAllCasesPaged(
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
    @GetMapping("/{caseId}")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable String caseId) {
        CaseDTO caseDTO = caseService.getCaseById(caseId);
        return ResponseEntity.ok(caseDTO);
    }
    
    /**
     * Update existing case
     */
    @PutMapping("/{caseId}")
    public ResponseEntity<CaseDTO> updateCase(@PathVariable String caseId,
                                              @Valid @RequestBody CaseDTO caseDTO) {
        CaseDTO updatedCase = caseService.updateCase(caseId, caseDTO);
        return ResponseEntity.ok(updatedCase);
    }
    
    /**
     * Delete case
     */
    @DeleteMapping("/{caseId}")
    public ResponseEntity<Void> deleteCase(@PathVariable String caseId) {
        caseService.deleteCase(caseId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get cases by assigned officer
     */
    @GetMapping("/by-officer/{assignedTo}")
    public ResponseEntity<List<CaseDTO>> getCasesByOfficer(@PathVariable String assignedTo) {
        List<CaseDTO> cases = caseService.getCasesByOfficer(assignedTo);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Search cases
     */
    @GetMapping("/search")
    public ResponseEntity<List<CaseDTO>> searchCases(@RequestParam String keyword) {
        List<CaseDTO> cases = caseService.searchCases(keyword);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Get case statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getCaseStatistics() {
        Map<String, Long> statistics = caseService.getCaseStatistics();
        return ResponseEntity.ok(statistics);
    }
}
