package com.merchantonboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.merchantonboarding.dto.BusinessTypeDTO;
import com.merchantonboarding.dto.MerchantCategoryDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.BusinessType;
import com.merchantonboarding.model.MerchantCategory;
import com.merchantonboarding.repository.BusinessTypeRepository;
import com.merchantonboarding.repository.MerchantCategoryRepository;

@ExtendWith(MockitoExtension.class)
class BusinessParamsServiceTest {

    @Mock private BusinessTypeRepository businessTypeRepository;
    @Mock private MerchantCategoryRepository merchantCategoryRepository;

    @InjectMocks
    private BusinessParamsService businessParamsService;

    private BusinessType testBusinessType;
    private MerchantCategory testMerchantCategory;

    @BeforeEach
    void setUp() {
        testBusinessType = new BusinessType();
        testBusinessType.setId("bt_1");
        testBusinessType.setCode("SDN");
        testBusinessType.setName("Sdn Bhd");
        testBusinessType.setDescription("Sendirian Berhad");
        testBusinessType.setStatus("active");

        testMerchantCategory = new MerchantCategory();
        testMerchantCategory.setId("mc_1");
        testMerchantCategory.setCode("RET");
        testMerchantCategory.setName("Retail");
        testMerchantCategory.setDescription("Retail businesses");
        testMerchantCategory.setRiskLevel("low");
        testMerchantCategory.setStatus("active");
    }

    // ═══════════════════════════════════════════════════════
    // Business Types
    // ═══════════════════════════════════════════════════════

    @Test
    void getAllBusinessTypes_ReturnsList() {
        when(businessTypeRepository.findAll()).thenReturn(List.of(testBusinessType));

        List<BusinessTypeDTO> result = businessParamsService.getAllBusinessTypes();

        assertEquals(1, result.size());
        assertEquals("Sdn Bhd", result.get(0).getName());
    }

    @Test
    void filterBusinessTypes_BySearch() {
        when(businessTypeRepository.searchBusinessTypes("Sdn")).thenReturn(List.of(testBusinessType));

        List<BusinessTypeDTO> result = businessParamsService.filterBusinessTypes("Sdn", null);

        assertEquals(1, result.size());
    }

    @Test
    void filterBusinessTypes_ByStatus() {
        when(businessTypeRepository.findAll()).thenReturn(List.of(testBusinessType));

        List<BusinessTypeDTO> result = businessParamsService.filterBusinessTypes(null, "active");

        assertEquals(1, result.size());
    }

    @Test
    void filterBusinessTypes_ByStatusFiltersOut() {
        when(businessTypeRepository.findAll()).thenReturn(List.of(testBusinessType));

        List<BusinessTypeDTO> result = businessParamsService.filterBusinessTypes(null, "inactive");

        assertEquals(0, result.size());
    }

    @Test
    void getBusinessTypeById_Found() {
        when(businessTypeRepository.findById("bt_1")).thenReturn(Optional.of(testBusinessType));

        BusinessTypeDTO result = businessParamsService.getBusinessTypeById("bt_1");

        assertEquals("SDN", result.getCode());
        assertEquals("Sdn Bhd", result.getName());
    }

    @Test
    void getBusinessTypeById_NotFound() {
        when(businessTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> businessParamsService.getBusinessTypeById("nonexistent"));
    }

    @Test
    void createBusinessType_Success() {
        BusinessTypeDTO dto = new BusinessTypeDTO();
        dto.setCode("ECOM");
        dto.setName("E-Commerce");
        dto.setDescription("Online businesses");

        when(businessTypeRepository.findByCode("ECOM")).thenReturn(Optional.empty());
        when(businessTypeRepository.save(any(BusinessType.class))).thenAnswer(inv -> inv.getArgument(0));

        BusinessTypeDTO result = businessParamsService.createBusinessType(dto);

        assertNotNull(result);
        assertEquals("E-Commerce", result.getName());
        assertEquals("active", result.getStatus());
    }

    @Test
    void createBusinessType_DuplicateCode() {
        BusinessTypeDTO dto = new BusinessTypeDTO();
        dto.setCode("SDN");
        dto.setName("Duplicate");

        when(businessTypeRepository.findByCode("SDN")).thenReturn(Optional.of(testBusinessType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> businessParamsService.createBusinessType(dto));
        assertEquals("Business type code already exists", ex.getMessage());
    }

    @Test
    void updateBusinessType_Success() {
        BusinessTypeDTO dto = new BusinessTypeDTO();
        dto.setCode("SDN");
        dto.setName("Updated Sdn Bhd");
        dto.setDescription("Updated description");
        dto.setStatus("active");

        when(businessTypeRepository.findById("bt_1")).thenReturn(Optional.of(testBusinessType));
        when(businessTypeRepository.save(any(BusinessType.class))).thenAnswer(inv -> inv.getArgument(0));

        BusinessTypeDTO result = businessParamsService.updateBusinessType("bt_1", dto);

        assertEquals("Updated Sdn Bhd", result.getName());
    }

    @Test
    void deleteBusinessType_Success() {
        when(businessTypeRepository.existsById("bt_1")).thenReturn(true);

        assertDoesNotThrow(() -> businessParamsService.deleteBusinessType("bt_1"));
        verify(businessTypeRepository).deleteById("bt_1");
    }

    @Test
    void deleteBusinessType_NotFound() {
        when(businessTypeRepository.existsById("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> businessParamsService.deleteBusinessType("nonexistent"));
    }

    // ═══════════════════════════════════════════════════════
    // Merchant Categories
    // ═══════════════════════════════════════════════════════

    @Test
    void getAllMerchantCategories_ReturnsList() {
        when(merchantCategoryRepository.findAll()).thenReturn(List.of(testMerchantCategory));

        List<MerchantCategoryDTO> result = businessParamsService.getAllMerchantCategories();

        assertEquals(1, result.size());
        assertEquals("Retail", result.get(0).getName());
        assertEquals("low", result.get(0).getRiskLevel());
    }

    @Test
    void filterMerchantCategories_ByRiskLevel() {
        when(merchantCategoryRepository.findAll()).thenReturn(List.of(testMerchantCategory));

        List<MerchantCategoryDTO> result =
                businessParamsService.filterMerchantCategories(null, null, "low");

        assertEquals(1, result.size());
    }

    @Test
    void filterMerchantCategories_ByRiskLevelFiltersOut() {
        when(merchantCategoryRepository.findAll()).thenReturn(List.of(testMerchantCategory));

        List<MerchantCategoryDTO> result =
                businessParamsService.filterMerchantCategories(null, null, "high");

        assertEquals(0, result.size());
    }

    @Test
    void createMerchantCategory_Success() {
        MerchantCategoryDTO dto = new MerchantCategoryDTO();
        dto.setCode("FNB");
        dto.setName("Food & Beverage");
        dto.setDescription("F&B businesses");
        dto.setRiskLevel("medium");

        when(merchantCategoryRepository.findByCode("FNB")).thenReturn(Optional.empty());
        when(merchantCategoryRepository.save(any(MerchantCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantCategoryDTO result = businessParamsService.createMerchantCategory(dto);

        assertNotNull(result);
        assertEquals("Food & Beverage", result.getName());
        assertEquals("medium", result.getRiskLevel());
    }

    @Test
    void createMerchantCategory_DuplicateCode() {
        MerchantCategoryDTO dto = new MerchantCategoryDTO();
        dto.setCode("RET");

        when(merchantCategoryRepository.findByCode("RET")).thenReturn(Optional.of(testMerchantCategory));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> businessParamsService.createMerchantCategory(dto));
        assertEquals("Merchant category code already exists", ex.getMessage());
    }

    @Test
    void updateMerchantCategory_Success() {
        MerchantCategoryDTO dto = new MerchantCategoryDTO();
        dto.setCode("RET");
        dto.setName("Updated Retail");
        dto.setDescription("Updated desc");
        dto.setRiskLevel("high");
        dto.setStatus("active");

        when(merchantCategoryRepository.findById("mc_1")).thenReturn(Optional.of(testMerchantCategory));
        when(merchantCategoryRepository.save(any(MerchantCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantCategoryDTO result = businessParamsService.updateMerchantCategory("mc_1", dto);

        assertEquals("Updated Retail", result.getName());
        assertEquals("high", result.getRiskLevel());
    }

    @Test
    void deleteMerchantCategory_Success() {
        when(merchantCategoryRepository.existsById("mc_1")).thenReturn(true);

        assertDoesNotThrow(() -> businessParamsService.deleteMerchantCategory("mc_1"));
        verify(merchantCategoryRepository).deleteById("mc_1");
    }

    @Test
    void deleteMerchantCategory_NotFound() {
        when(merchantCategoryRepository.existsById("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> businessParamsService.deleteMerchantCategory("nonexistent"));
    }
}
