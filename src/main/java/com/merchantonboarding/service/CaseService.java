package com.merchantonboarding.service;

import com.merchantonboarding.dto.CaseDTO;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.OnboardingCase.CaseStatus;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaseService {
    
    @Autowired
    private CaseRepository caseRepository;
    
    /**
     * Get all cases with pagination and filtering
     * Implements basic CRUD operations for assignment requirements
     */
    public Page<CaseDTO> getAllCases(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OnboardingCase> casePage;
        
        if (status != null && !status.isEmpty()) {
            CaseStatus caseStatus = CaseStatus.valueOf(status);
            casePage = caseRepository.findByStatusOrderByCreatedAtDesc(caseStatus, pageable);
        } else {
            casePage = caseRepository.findAll(pageable);
        }
        
        // Fixed: Correct method reference
        return casePage.map(this::convertToDTO);
    }
    
    /**
     * Create new case - implements CRUD operations
     */
    public CaseDTO createCase(CaseDTO caseDTO) {
        OnboardingCase newCase = convertToEntity(caseDTO);
        OnboardingCase savedCase = caseRepository.save(newCase);
        // Fixed: Correct method call
        return convertToDTO(savedCase);
    }
    
    /**
     * Get case by ID
     */
    public CaseDTO getCaseById(Long id) {
        // Fixed: Don't use 'case' as variable name (it's a reserved keyword)
        OnboardingCase onboardingCase = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));
        return convertToDTO(onboardingCase);
    }
    
    /**
     * Update case - implements CRUD operations
     */
    public CaseDTO updateCase(Long id, CaseDTO caseDTO) {
        OnboardingCase existingCase = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));
        
        // Update fields
        existingCase.setMerchantName(caseDTO.getMerchantName());
        existingCase.setBusinessType(caseDTO.getBusinessType());
        existingCase.setContactPerson(caseDTO.getContactPerson());
        existingCase.setPhoneNumber(caseDTO.getPhoneNumber());
        existingCase.setEmail(caseDTO.getEmail());
        existingCase.setAddress(caseDTO.getAddress());
        
        OnboardingCase updatedCase = caseRepository.save(existingCase);
        return convertToDTO(updatedCase);
    }
    
    /**
     * Delete case - implements CRUD operations
     */
    public void deleteCase(Long id) {
        if (!caseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Case not found with id: " + id);
        }
        caseRepository.deleteById(id);
    }
    
    /**
     * Get cases by officer - derived query implementation
     */
    public List<CaseDTO> getCasesByOfficer(Long officerId) {
        List<OnboardingCase> cases = caseRepository.findByAssignedOfficerId(officerId);
        // Fixed: Correct stream mapping
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Search cases - JPQL query implementation
     */
    public List<CaseDTO> searchCases(String keyword) {
        List<OnboardingCase> cases = caseRepository.searchCases(keyword);
        // Fixed: Correct stream mapping
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get case statistics - native SQL query implementation
     */
    public Map<String, Long> getCaseStatistics() {
        List<Object[]> results = caseRepository.getCaseStatusStatistics();
        return results.stream()
            .collect(Collectors.toMap(
                result -> (String) result[0],
                result -> ((Number) result[1]).longValue()
            ));
    }
    
    // Helper methods for DTO conversion
    // Fixed: Correct method signature and parameter name
    private CaseDTO convertToDTO(OnboardingCase onboardingCase) {
        CaseDTO dto = new CaseDTO();
        dto.setId(onboardingCase.getId());
        dto.setMerchantName(onboardingCase.getMerchantName());
        dto.setBusinessType(onboardingCase.getBusinessType());
        dto.setContactPerson(onboardingCase.getContactPerson());
        dto.setPhoneNumber(onboardingCase.getPhoneNumber());
        dto.setEmail(onboardingCase.getEmail());
        dto.setAddress(onboardingCase.getAddress());
        dto.setStatus(onboardingCase.getStatus().toString());
        dto.setCreatedAt(onboardingCase.getCreatedAt());
        dto.setUpdatedAt(onboardingCase.getUpdatedAt());
        
        // Set assigned officer ID if exists
        if (onboardingCase.getAssignedOfficer() != null) {
            dto.setAssignedOfficerId(onboardingCase.getAssignedOfficer().getId());
        }
        
        return dto;
    }
    
    // Fixed: Correct method signature and parameter name
    private OnboardingCase convertToEntity(CaseDTO dto) {
        OnboardingCase onboardingCase = new OnboardingCase();
        onboardingCase.setMerchantName(dto.getMerchantName());
        onboardingCase.setBusinessType(dto.getBusinessType());
        onboardingCase.setContactPerson(dto.getContactPerson());
        onboardingCase.setPhoneNumber(dto.getPhoneNumber());
        onboardingCase.setEmail(dto.getEmail());
        onboardingCase.setAddress(dto.getAddress());
        
        // Set status if provided, otherwise default to PENDING
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            onboardingCase.setStatus(CaseStatus.valueOf(dto.getStatus()));
        } else {
            onboardingCase.setStatus(CaseStatus.PENDING);
        }
        
        return onboardingCase;
    }
}
