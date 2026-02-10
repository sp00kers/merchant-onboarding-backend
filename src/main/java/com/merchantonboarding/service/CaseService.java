package com.merchantonboarding.service;

import com.merchantonboarding.dto.CaseDTO;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.Document;
import com.merchantonboarding.model.CaseHistory;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaseService {
    
    @Autowired
    private CaseRepository caseRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Get all cases with pagination and filtering
     */
    public Page<CaseDTO> getAllCases(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OnboardingCase> casePage;
        
        if (status != null && !status.isEmpty()) {
            casePage = caseRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            casePage = caseRepository.findAll(pageable);
        }
        
        return casePage.map(this::convertToDTO);
    }
    
    /**
     * Get all cases as list (for frontend compatibility)
     */
    public List<CaseDTO> getAllCasesAsList() {
        return caseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Create new case
     */
    public CaseDTO createCase(CaseDTO caseDTO) {
        OnboardingCase newCase = convertToEntity(caseDTO);

        // Generate case ID if not provided
        if (newCase.getCaseId() == null || newCase.getCaseId().isEmpty()) {
            newCase.setCaseId(generateCaseId());
        }

        // Add initial history entry
        CaseHistory historyEntry = new CaseHistory();
        historyEntry.setTime(LocalDateTime.now().format(DATETIME_FORMATTER));
        historyEntry.setAction("Case created by " + (caseDTO.getAssignedTo() != null ? caseDTO.getAssignedTo() : "System"));
        historyEntry.setOnboardingCase(newCase);
        newCase.getHistory().add(historyEntry);

        OnboardingCase savedCase = caseRepository.save(newCase);
        return convertToDTO(savedCase);
    }
    
    /**
     * Get case by ID
     */
    public CaseDTO getCaseById(String caseId) {
        OnboardingCase onboardingCase = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        return convertToDTO(onboardingCase);
    }
    
    /**
     * Update case
     */
    public CaseDTO updateCase(String caseId, CaseDTO caseDTO) {
        OnboardingCase existingCase = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        // Track status change for history
        String oldStatus = existingCase.getStatus();

        // Update fields
        existingCase.setBusinessName(caseDTO.getBusinessName());
        existingCase.setBusinessType(caseDTO.getBusinessType());
        existingCase.setRegistrationNumber(caseDTO.getRegistrationNumber());
        existingCase.setMerchantCategory(caseDTO.getMerchantCategory());
        existingCase.setBusinessAddress(caseDTO.getBusinessAddress());
        existingCase.setDirectorName(caseDTO.getDirectorName());
        existingCase.setDirectorIC(caseDTO.getDirectorIC());
        existingCase.setDirectorPhone(caseDTO.getDirectorPhone());
        existingCase.setDirectorEmail(caseDTO.getDirectorEmail());
        existingCase.setAssignedTo(caseDTO.getAssignedTo());
        existingCase.setPriority(caseDTO.getPriority());

        if (caseDTO.getStatus() != null) {
            existingCase.setStatus(caseDTO.getStatus());
        }

        // Add history entry if status changed
        if (caseDTO.getStatus() != null && !caseDTO.getStatus().equals(oldStatus)) {
            CaseHistory historyEntry = new CaseHistory();
            historyEntry.setTime(LocalDateTime.now().format(DATETIME_FORMATTER));
            historyEntry.setAction("Status changed from '" + oldStatus + "' to '" + caseDTO.getStatus() + "'");
            historyEntry.setOnboardingCase(existingCase);
            existingCase.getHistory().add(historyEntry);
        }

        OnboardingCase updatedCase = caseRepository.save(existingCase);
        return convertToDTO(updatedCase);
    }
    
    /**
     * Delete case
     */
    public void deleteCase(String caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case not found with id: " + caseId);
        }
        caseRepository.deleteById(caseId);
    }
    
    /**
     * Get cases by assigned officer
     */
    public List<CaseDTO> getCasesByOfficer(String assignedTo) {
        List<OnboardingCase> cases = caseRepository.findByAssignedTo(assignedTo);
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Search cases
     */
    public List<CaseDTO> searchCases(String keyword) {
        List<OnboardingCase> cases = caseRepository.searchCases(keyword);
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Filter cases
     */
    public List<CaseDTO> filterCases(String status, String searchTerm) {
        List<OnboardingCase> cases;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            cases = caseRepository.searchCases(searchTerm);
        } else {
            cases = caseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (status != null && !status.isEmpty()) {
            cases = cases.stream()
                .filter(c -> c.getStatus().equals(status))
                .collect(Collectors.toList());
        }

        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get case statistics
     */
    public Map<String, Long> getCaseStatistics() {
        List<Object[]> results = caseRepository.getCaseStatusStatistics();
        return results.stream()
            .collect(Collectors.toMap(
                result -> (String) result[0],
                result -> ((Number) result[1]).longValue()
            ));
    }
    
    /**
     * Add history entry to a case
     */
    public void addHistoryEntry(String caseId, String action) {
        OnboardingCase onboardingCase = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        CaseHistory historyEntry = new CaseHistory();
        historyEntry.setTime(LocalDateTime.now().format(DATETIME_FORMATTER));
        historyEntry.setAction(action);
        historyEntry.setOnboardingCase(onboardingCase);
        onboardingCase.getHistory().add(historyEntry);

        caseRepository.save(onboardingCase);
    }

    private String generateCaseId() {
        int year = LocalDateTime.now().getYear();
        long count = caseRepository.count() + 1;
        return String.format("MOP-%d-%03d", year, count);
    }

    private CaseDTO convertToDTO(OnboardingCase c) {
        CaseDTO dto = new CaseDTO();
        dto.setCaseId(c.getCaseId());
        dto.setBusinessName(c.getBusinessName());
        dto.setBusinessType(c.getBusinessType());
        dto.setRegistrationNumber(c.getRegistrationNumber());
        dto.setMerchantCategory(c.getMerchantCategory());
        dto.setBusinessAddress(c.getBusinessAddress());
        dto.setDirectorName(c.getDirectorName());
        dto.setDirectorIC(c.getDirectorIC());
        dto.setDirectorPhone(c.getDirectorPhone());
        dto.setDirectorEmail(c.getDirectorEmail());
        dto.setStatus(c.getStatus());
        dto.setCreatedDate(c.getCreatedDate());
        dto.setAssignedTo(c.getAssignedTo());
        dto.setPriority(c.getPriority());
        dto.setLastUpdated(c.getLastUpdated());

        // Convert documents
        if (c.getDocuments() != null) {
            dto.setDocuments(c.getDocuments().stream()
                .map(d -> {
                    CaseDTO.DocumentDTO docDTO = new CaseDTO.DocumentDTO();
                    docDTO.setName(d.getName());
                    docDTO.setType(d.getType());
                    docDTO.setUploadedAt(d.getUploadedAt());
                    return docDTO;
                })
                .collect(Collectors.toList()));
        }

        // Convert history
        if (c.getHistory() != null) {
            dto.setHistory(c.getHistory().stream()
                .map(h -> {
                    CaseDTO.CaseHistoryDTO historyDTO = new CaseDTO.CaseHistoryDTO();
                    historyDTO.setTime(h.getTime());
                    historyDTO.setAction(h.getAction());
                    return historyDTO;
                })
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private OnboardingCase convertToEntity(CaseDTO dto) {
        OnboardingCase c = new OnboardingCase();
        c.setCaseId(dto.getCaseId());
        c.setBusinessName(dto.getBusinessName());
        c.setBusinessType(dto.getBusinessType());
        c.setRegistrationNumber(dto.getRegistrationNumber());
        c.setMerchantCategory(dto.getMerchantCategory());
        c.setBusinessAddress(dto.getBusinessAddress());
        c.setDirectorName(dto.getDirectorName());
        c.setDirectorIC(dto.getDirectorIC());
        c.setDirectorPhone(dto.getDirectorPhone());
        c.setDirectorEmail(dto.getDirectorEmail());
        c.setAssignedTo(dto.getAssignedTo());
        c.setPriority(dto.getPriority() != null ? dto.getPriority() : "Normal");
        c.setStatus(dto.getStatus() != null ? dto.getStatus() : "Pending Review");
        c.setCreatedDate(dto.getCreatedDate() != null ? dto.getCreatedDate() : LocalDateTime.now().format(DATE_FORMATTER));

        return c;
    }
}
