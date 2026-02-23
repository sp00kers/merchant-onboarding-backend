package com.merchantonboarding.controller;

import com.merchantonboarding.dto.CaseDTO;
import com.merchantonboarding.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * Requires CASE_VIEW permission
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<CaseDTO>> getAllCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        List<CaseDTO> cases = caseService.filterCases(status, search);
        return ResponseEntity.ok(cases);
    }

    /**
     * Get all cases with pagination
     * Requires CASE_VIEW permission
     */
    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Page<CaseDTO>> getAllCasesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        Page<CaseDTO> cases = caseService.getAllCases(page, size, status);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Create new merchant onboarding case
     * Requires CASE_CREATION permission
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> createCase(@Valid @RequestBody CaseDTO caseDTO) {
        CaseDTO createdCase = caseService.createCase(caseDTO);
        return ResponseEntity.ok(createdCase);
    }
    
    /**
     * Get case by ID with path parameter
     * Requires CASE_VIEW permission
     */
    @GetMapping("/{caseId}")
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable String caseId) {
        CaseDTO caseDTO = caseService.getCaseById(caseId);
        return ResponseEntity.ok(caseDTO);
    }
    
    /**
     * Update existing case
     * Requires CASE_MANAGEMENT permission
     */
    @PutMapping("/{caseId}")
    @PreAuthorize("hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> updateCase(@PathVariable String caseId,
                                              @Valid @RequestBody CaseDTO caseDTO) {
        CaseDTO updatedCase = caseService.updateCase(caseId, caseDTO);
        return ResponseEntity.ok(updatedCase);
    }
    
    /**
     * Delete case
     * Requires CASE_MANAGEMENT permission
     */
    @DeleteMapping("/{caseId}")
    @PreAuthorize("hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Void> deleteCase(@PathVariable String caseId) {
        caseService.deleteCase(caseId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get cases by assigned officer
     * Requires CASE_VIEW permission
     */
    @GetMapping("/by-officer/{assignedTo}")
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<CaseDTO>> getCasesByOfficer(@PathVariable String assignedTo) {
        List<CaseDTO> cases = caseService.getCasesByOfficer(assignedTo);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Search cases
     * Requires CASE_VIEW permission
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<CaseDTO>> searchCases(@RequestParam String keyword) {
        List<CaseDTO> cases = caseService.searchCases(keyword);
        return ResponseEntity.ok(cases);
    }
    
    /**
     * Get case statistics
     * Requires CASE_VIEW permission
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Map<String, Long>> getCaseStatistics() {
        Map<String, Long> statistics = caseService.getCaseStatistics();
        return ResponseEntity.ok(statistics);
    }
}
