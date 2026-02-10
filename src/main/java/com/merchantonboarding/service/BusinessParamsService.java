package com.merchantonboarding.service;

import com.merchantonboarding.dto.BusinessTypeDTO;
import com.merchantonboarding.dto.MerchantCategoryDTO;
import com.merchantonboarding.dto.RiskCategoryDTO;
import com.merchantonboarding.model.BusinessType;
import com.merchantonboarding.model.MerchantCategory;
import com.merchantonboarding.model.RiskCategory;
import com.merchantonboarding.repository.BusinessTypeRepository;
import com.merchantonboarding.repository.MerchantCategoryRepository;
import com.merchantonboarding.repository.RiskCategoryRepository;
import com.merchantonboarding.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BusinessParamsService {

    @Autowired
    private BusinessTypeRepository businessTypeRepository;

    @Autowired
    private MerchantCategoryRepository merchantCategoryRepository;

    @Autowired
    private RiskCategoryRepository riskCategoryRepository;

    // ─── Business Types ───────────────────────────────────────

    public List<BusinessTypeDTO> getAllBusinessTypes() {
        return businessTypeRepository.findAll().stream()
            .map(this::convertBusinessTypeToDTO)
            .collect(Collectors.toList());
    }

    public List<BusinessTypeDTO> filterBusinessTypes(String search, String status) {
        List<BusinessType> items;

        if (search != null && !search.isEmpty()) {
            items = businessTypeRepository.searchBusinessTypes(search);
        } else {
            items = businessTypeRepository.findAll();
        }

        if (status != null && !status.isEmpty()) {
            items = items.stream()
                .filter(bt -> bt.getStatus().equals(status))
                .collect(Collectors.toList());
        }

        return items.stream()
            .map(this::convertBusinessTypeToDTO)
            .collect(Collectors.toList());
    }

    public BusinessTypeDTO getBusinessTypeById(String id) {
        BusinessType bt = businessTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Business type not found: " + id));
        return convertBusinessTypeToDTO(bt);
    }

    public BusinessTypeDTO createBusinessType(BusinessTypeDTO dto) {
        // Check for duplicate code
        if (businessTypeRepository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Business type code already exists");
        }

        BusinessType bt = new BusinessType();
        bt.setId(dto.getId() != null ? dto.getId() : "bt_" + System.currentTimeMillis());
        bt.setCode(dto.getCode());
        bt.setName(dto.getName());
        bt.setDescription(dto.getDescription());
        bt.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");

        BusinessType saved = businessTypeRepository.save(bt);
        return convertBusinessTypeToDTO(saved);
    }

    public BusinessTypeDTO updateBusinessType(String id, BusinessTypeDTO dto) {
        BusinessType bt = businessTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Business type not found: " + id));

        bt.setCode(dto.getCode());
        bt.setName(dto.getName());
        bt.setDescription(dto.getDescription());
        bt.setStatus(dto.getStatus());

        BusinessType saved = businessTypeRepository.save(bt);
        return convertBusinessTypeToDTO(saved);
    }

    public void deleteBusinessType(String id) {
        if (!businessTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Business type not found: " + id);
        }
        businessTypeRepository.deleteById(id);
    }

    // ─── Merchant Categories ───────────────────────────────────────

    public List<MerchantCategoryDTO> getAllMerchantCategories() {
        return merchantCategoryRepository.findAll().stream()
            .map(this::convertMerchantCategoryToDTO)
            .collect(Collectors.toList());
    }

    public List<MerchantCategoryDTO> filterMerchantCategories(String search, String status, String riskLevel) {
        List<MerchantCategory> items;

        if (search != null && !search.isEmpty()) {
            items = merchantCategoryRepository.searchMerchantCategories(search);
        } else {
            items = merchantCategoryRepository.findAll();
        }

        if (status != null && !status.isEmpty()) {
            items = items.stream()
                .filter(mc -> mc.getStatus().equals(status))
                .collect(Collectors.toList());
        }

        if (riskLevel != null && !riskLevel.isEmpty()) {
            items = items.stream()
                .filter(mc -> mc.getRiskLevel().equals(riskLevel))
                .collect(Collectors.toList());
        }

        return items.stream()
            .map(this::convertMerchantCategoryToDTO)
            .collect(Collectors.toList());
    }

    public MerchantCategoryDTO getMerchantCategoryById(String id) {
        MerchantCategory mc = merchantCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant category not found: " + id));
        return convertMerchantCategoryToDTO(mc);
    }

    public MerchantCategoryDTO createMerchantCategory(MerchantCategoryDTO dto) {
        // Check for duplicate code
        if (merchantCategoryRepository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Merchant category code already exists");
        }

        MerchantCategory mc = new MerchantCategory();
        mc.setId(dto.getId() != null ? dto.getId() : "mc_" + System.currentTimeMillis());
        mc.setCode(dto.getCode());
        mc.setName(dto.getName());
        mc.setDescription(dto.getDescription());
        mc.setRiskLevel(dto.getRiskLevel() != null ? dto.getRiskLevel() : "low");
        mc.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");

        MerchantCategory saved = merchantCategoryRepository.save(mc);
        return convertMerchantCategoryToDTO(saved);
    }

    public MerchantCategoryDTO updateMerchantCategory(String id, MerchantCategoryDTO dto) {
        MerchantCategory mc = merchantCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant category not found: " + id));

        mc.setCode(dto.getCode());
        mc.setName(dto.getName());
        mc.setDescription(dto.getDescription());
        mc.setRiskLevel(dto.getRiskLevel());
        mc.setStatus(dto.getStatus());

        MerchantCategory saved = merchantCategoryRepository.save(mc);
        return convertMerchantCategoryToDTO(saved);
    }

    public void deleteMerchantCategory(String id) {
        if (!merchantCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Merchant category not found: " + id);
        }
        merchantCategoryRepository.deleteById(id);
    }

    // ─── Risk Categories ───────────────────────────────────────

    public List<RiskCategoryDTO> getAllRiskCategories() {
        return riskCategoryRepository.findAllByOrderByLevelAsc().stream()
            .map(this::convertRiskCategoryToDTO)
            .collect(Collectors.toList());
    }

    public RiskCategoryDTO getRiskCategoryById(String id) {
        RiskCategory rc = riskCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Risk category not found: " + id));
        return convertRiskCategoryToDTO(rc);
    }

    public RiskCategoryDTO createRiskCategory(RiskCategoryDTO dto) {
        // Check for duplicate level
        if (riskCategoryRepository.findByLevel(dto.getLevel()).isPresent()) {
            throw new RuntimeException("Risk category level already exists");
        }

        RiskCategory rc = new RiskCategory();
        rc.setId(dto.getId() != null ? dto.getId() : "rc_" + System.currentTimeMillis());
        rc.setLevel(dto.getLevel());
        rc.setName(dto.getName());
        rc.setScoreRange(dto.getScoreRange());
        rc.setDescription(dto.getDescription());
        rc.setActionsRequired(dto.getActionsRequired());

        RiskCategory saved = riskCategoryRepository.save(rc);
        return convertRiskCategoryToDTO(saved);
    }

    public RiskCategoryDTO updateRiskCategory(String id, RiskCategoryDTO dto) {
        RiskCategory rc = riskCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Risk category not found: " + id));

        rc.setLevel(dto.getLevel());
        rc.setName(dto.getName());
        rc.setScoreRange(dto.getScoreRange());
        rc.setDescription(dto.getDescription());
        rc.setActionsRequired(dto.getActionsRequired());

        RiskCategory saved = riskCategoryRepository.save(rc);
        return convertRiskCategoryToDTO(saved);
    }

    public void deleteRiskCategory(String id) {
        if (!riskCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Risk category not found: " + id);
        }
        riskCategoryRepository.deleteById(id);
    }

    // ─── Conversion Methods ───────────────────────────────────────

    private BusinessTypeDTO convertBusinessTypeToDTO(BusinessType bt) {
        BusinessTypeDTO dto = new BusinessTypeDTO();
        dto.setId(bt.getId());
        dto.setCode(bt.getCode());
        dto.setName(bt.getName());
        dto.setDescription(bt.getDescription());
        dto.setStatus(bt.getStatus());
        dto.setCreatedAt(bt.getCreatedAt() != null ? bt.getCreatedAt().toString() : null);
        dto.setUpdatedAt(bt.getUpdatedAt() != null ? bt.getUpdatedAt().toString() : null);
        return dto;
    }

    private MerchantCategoryDTO convertMerchantCategoryToDTO(MerchantCategory mc) {
        MerchantCategoryDTO dto = new MerchantCategoryDTO();
        dto.setId(mc.getId());
        dto.setCode(mc.getCode());
        dto.setName(mc.getName());
        dto.setDescription(mc.getDescription());
        dto.setRiskLevel(mc.getRiskLevel());
        dto.setStatus(mc.getStatus());
        dto.setCreatedAt(mc.getCreatedAt() != null ? mc.getCreatedAt().toString() : null);
        dto.setUpdatedAt(mc.getUpdatedAt() != null ? mc.getUpdatedAt().toString() : null);
        return dto;
    }

    private RiskCategoryDTO convertRiskCategoryToDTO(RiskCategory rc) {
        RiskCategoryDTO dto = new RiskCategoryDTO();
        dto.setId(rc.getId());
        dto.setLevel(rc.getLevel());
        dto.setName(rc.getName());
        dto.setScoreRange(rc.getScoreRange());
        dto.setDescription(rc.getDescription());
        dto.setActionsRequired(rc.getActionsRequired());
        dto.setCreatedAt(rc.getCreatedAt() != null ? rc.getCreatedAt().toString() : null);
        dto.setUpdatedAt(rc.getUpdatedAt() != null ? rc.getUpdatedAt().toString() : null);
        return dto;
    }
}

