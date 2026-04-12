package com.merchantonboarding.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.merchantonboarding.dto.CaseDTO;
import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.UserRepository;
import com.merchantonboarding.service.CaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "http://localhost:4200")
public class CaseController {
    
    @Autowired
    private CaseService caseService;

    @Autowired
    private UserRepository userRepository;
    
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
     * Save case as draft (no field validation required)
     * Requires CASE_CREATION permission
     */
    @PostMapping("/draft")
    @PreAuthorize("hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> saveDraft(@RequestBody CaseDTO caseDTO) {
        CaseDTO createdCase = caseService.saveDraft(caseDTO);
        return ResponseEntity.ok(createdCase);
    }

    /**
     * Update draft case (no field validation required)
     * Requires CASE_MANAGEMENT permission
     */
    @PutMapping("/{caseId}/draft")
    @PreAuthorize("hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> updateDraft(@PathVariable String caseId,
                                               @RequestBody CaseDTO caseDTO) {
        CaseDTO updatedCase = caseService.updateDraft(caseId, caseDTO);
        return ResponseEntity.ok(updatedCase);
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

    /**
     * Update case status
     * Requires CASE_MANAGEMENT permission.
     * Admins can approve/reject any case.
     * Compliance reviewers can only approve/reject cases assigned to them.
     */
    @PatchMapping("/{caseId}/status")
    @PreAuthorize("hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> updateCaseStatus(@PathVariable String caseId,
                                                    @RequestBody Map<String, String> request) {
        String status = request.get("status");

        // Enforce: only the assigned compliance reviewer (or admin) can approve/reject
        if ("Approved".equals(status) || "Rejected".equals(status)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ALL_MODULES".equals(a.getAuthority()));

            if (!isAdmin) {
                // Look up current user's name from their email (principal)
                String email = auth.getName();
                String currentUserName = userRepository.findByEmail(email)
                        .map(User::getName)
                        .orElse(null);

                CaseDTO caseData = caseService.getCaseById(caseId);
                if (currentUserName == null || !currentUserName.equals(caseData.getAssignedTo())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
        }

        CaseDTO updatedCase = caseService.updateCaseStatus(caseId, status);
        return ResponseEntity.ok(updatedCase);
    }

    /**
     * Add history entry to a case
     * Requires CASE_VIEW or CASE_MANAGEMENT permission
     */
    @PostMapping("/{caseId}/history")
    @PreAuthorize("hasAuthority('CASE_VIEW') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> addHistoryEntry(@PathVariable String caseId,
                                                   @RequestBody Map<String, String> request) {
        String action = request.get("action");
        caseService.addHistoryEntry(caseId, action);
        CaseDTO updatedCase = caseService.getCaseById(caseId);
        return ResponseEntity.ok(updatedCase);
    }

    /**
     * Assign case to a reviewer
     * Requires CASE_MANAGEMENT or CASE_CREATION permission
     */
    @PatchMapping("/{caseId}/assign")
    @PreAuthorize("hasAuthority('CASE_MANAGEMENT') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> assignCase(@PathVariable String caseId,
                                              @RequestBody Map<String, String> request) {
        String assignedTo = request.get("assignedTo");
        CaseDTO updatedCase = caseService.assignCase(caseId, assignedTo);
        return ResponseEntity.ok(updatedCase);
    }

    /**
     * Upload documents for a case
     * Requires CASE_CREATION or DOCUMENT_UPLOAD permission
     */
    @PostMapping(value = "/{caseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CASE_CREATION') or hasAuthority('DOCUMENT_UPLOAD') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<CaseDTO> uploadDocuments(
            @PathVariable String caseId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "types", required = false) String[] types) {
        CaseDTO updatedCase = caseService.uploadDocuments(caseId, files, types);
        return ResponseEntity.ok(updatedCase);
    }
}
