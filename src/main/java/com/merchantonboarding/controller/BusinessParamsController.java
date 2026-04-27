package com.merchantonboarding.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.BusinessTypeDTO;
import com.merchantonboarding.dto.MerchantCategoryDTO;
import com.merchantonboarding.service.BusinessParamsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/business-params")
@CrossOrigin(origins = "http://localhost:4200")
public class BusinessParamsController {

    @Autowired
    private BusinessParamsService businessParamsService;

    // ─── Business Types ───────────────────────────────────────

    /**
     * Get all business types - read access for case creation
     */
    @GetMapping("/business-types")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<BusinessTypeDTO>> getAllBusinessTypes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<BusinessTypeDTO> types = businessParamsService.filterBusinessTypes(search, status);
        return ResponseEntity.ok(types);
    }

    @GetMapping("/business-types/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<BusinessTypeDTO> getBusinessTypeById(@PathVariable String id) {
        BusinessTypeDTO type = businessParamsService.getBusinessTypeById(id);
        return ResponseEntity.ok(type);
    }

    @PostMapping("/business-types")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<BusinessTypeDTO> createBusinessType(@Valid @RequestBody BusinessTypeDTO dto) {
        BusinessTypeDTO created = businessParamsService.createBusinessType(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/business-types/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<BusinessTypeDTO> updateBusinessType(@PathVariable String id,
                                                              @Valid @RequestBody BusinessTypeDTO dto) {
        BusinessTypeDTO updated = businessParamsService.updateBusinessType(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/business-types/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Void> deleteBusinessType(@PathVariable String id) {
        businessParamsService.deleteBusinessType(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Merchant Categories ───────────────────────────────────────

    /**
     * Get all merchant categories - read access for case creation
     */
    @GetMapping("/merchant-categories")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<MerchantCategoryDTO>> getAllMerchantCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel) {
        List<MerchantCategoryDTO> categories = businessParamsService.filterMerchantCategories(search, status, riskLevel);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/merchant-categories/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('CASE_CREATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<MerchantCategoryDTO> getMerchantCategoryById(@PathVariable String id) {
        MerchantCategoryDTO category = businessParamsService.getMerchantCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/merchant-categories")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<MerchantCategoryDTO> createMerchantCategory(@Valid @RequestBody MerchantCategoryDTO dto) {
        MerchantCategoryDTO created = businessParamsService.createMerchantCategory(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/merchant-categories/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<MerchantCategoryDTO> updateMerchantCategory(@PathVariable String id,
                                                                      @Valid @RequestBody MerchantCategoryDTO dto) {
        MerchantCategoryDTO updated = businessParamsService.updateMerchantCategory(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/merchant-categories/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURATION') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Void> deleteMerchantCategory(@PathVariable String id) {
        businessParamsService.deleteMerchantCategory(id);
        return ResponseEntity.noContent().build();
    }
}

