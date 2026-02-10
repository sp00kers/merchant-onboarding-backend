package com.merchantonboarding.controller;

import com.merchantonboarding.dto.BusinessTypeDTO;
import com.merchantonboarding.dto.MerchantCategoryDTO;
import com.merchantonboarding.dto.RiskCategoryDTO;
import com.merchantonboarding.service.BusinessParamsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/business-params")
@CrossOrigin(origins = "http://localhost:4200")
public class BusinessParamsController {

    @Autowired
    private BusinessParamsService businessParamsService;

    // ─── Business Types ───────────────────────────────────────

    @GetMapping("/business-types")
    public ResponseEntity<List<BusinessTypeDTO>> getAllBusinessTypes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<BusinessTypeDTO> types = businessParamsService.filterBusinessTypes(search, status);
        return ResponseEntity.ok(types);
    }

    @GetMapping("/business-types/{id}")
    public ResponseEntity<BusinessTypeDTO> getBusinessTypeById(@PathVariable String id) {
        BusinessTypeDTO type = businessParamsService.getBusinessTypeById(id);
        return ResponseEntity.ok(type);
    }

    @PostMapping("/business-types")
    public ResponseEntity<BusinessTypeDTO> createBusinessType(@Valid @RequestBody BusinessTypeDTO dto) {
        BusinessTypeDTO created = businessParamsService.createBusinessType(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/business-types/{id}")
    public ResponseEntity<BusinessTypeDTO> updateBusinessType(@PathVariable String id,
                                                              @Valid @RequestBody BusinessTypeDTO dto) {
        BusinessTypeDTO updated = businessParamsService.updateBusinessType(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/business-types/{id}")
    public ResponseEntity<Void> deleteBusinessType(@PathVariable String id) {
        businessParamsService.deleteBusinessType(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Merchant Categories ───────────────────────────────────────

    @GetMapping("/merchant-categories")
    public ResponseEntity<List<MerchantCategoryDTO>> getAllMerchantCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel) {
        List<MerchantCategoryDTO> categories = businessParamsService.filterMerchantCategories(search, status, riskLevel);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/merchant-categories/{id}")
    public ResponseEntity<MerchantCategoryDTO> getMerchantCategoryById(@PathVariable String id) {
        MerchantCategoryDTO category = businessParamsService.getMerchantCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/merchant-categories")
    public ResponseEntity<MerchantCategoryDTO> createMerchantCategory(@Valid @RequestBody MerchantCategoryDTO dto) {
        MerchantCategoryDTO created = businessParamsService.createMerchantCategory(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/merchant-categories/{id}")
    public ResponseEntity<MerchantCategoryDTO> updateMerchantCategory(@PathVariable String id,
                                                                      @Valid @RequestBody MerchantCategoryDTO dto) {
        MerchantCategoryDTO updated = businessParamsService.updateMerchantCategory(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/merchant-categories/{id}")
    public ResponseEntity<Void> deleteMerchantCategory(@PathVariable String id) {
        businessParamsService.deleteMerchantCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Risk Categories ───────────────────────────────────────

    @GetMapping("/risk-categories")
    public ResponseEntity<List<RiskCategoryDTO>> getAllRiskCategories() {
        List<RiskCategoryDTO> categories = businessParamsService.getAllRiskCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/risk-categories/{id}")
    public ResponseEntity<RiskCategoryDTO> getRiskCategoryById(@PathVariable String id) {
        RiskCategoryDTO category = businessParamsService.getRiskCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/risk-categories")
    public ResponseEntity<RiskCategoryDTO> createRiskCategory(@Valid @RequestBody RiskCategoryDTO dto) {
        RiskCategoryDTO created = businessParamsService.createRiskCategory(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/risk-categories/{id}")
    public ResponseEntity<RiskCategoryDTO> updateRiskCategory(@PathVariable String id,
                                                              @Valid @RequestBody RiskCategoryDTO dto) {
        RiskCategoryDTO updated = businessParamsService.updateRiskCategory(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/risk-categories/{id}")
    public ResponseEntity<Void> deleteRiskCategory(@PathVariable String id) {
        businessParamsService.deleteRiskCategory(id);
        return ResponseEntity.noContent().build();
    }
}

