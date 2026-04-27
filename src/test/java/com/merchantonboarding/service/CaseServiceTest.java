package com.merchantonboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.merchantonboarding.dto.CaseDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.DocumentRepository;
import com.merchantonboarding.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CaseService caseService;

    @TempDir
    Path tempDir;

    private OnboardingCase testCase;
    private CaseDTO testCaseDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(caseService, "uploadDir", tempDir.toString());

        testCase = new OnboardingCase();
        testCase.setCaseId("MOP-2026-001");
        testCase.setBusinessName("ABC Trading Sdn Bhd");
        testCase.setBusinessType("Sdn Bhd");
        testCase.setRegistrationNumber("123456789012");
        testCase.setMerchantCategory("Retail");
        testCase.setBusinessAddress("No. 123, Jalan Kaya, 50200 Kuala Lumpur");
        testCase.setDirectorName("John Doe");
        testCase.setDirectorIC("900101141234");
        testCase.setDirectorPhone("0121234567");
        testCase.setDirectorEmail("john@gmail.com");
        testCase.setStatus("Pending Review");
        testCase.setAssignedTo("Sarah Lee");
        testCase.setCreatedDate("2026-04-01");
        testCase.setDocuments(new ArrayList<>());
        testCase.setHistory(new ArrayList<>());

        testCaseDTO = new CaseDTO();
        testCaseDTO.setBusinessName("ABC Trading Sdn Bhd");
        testCaseDTO.setBusinessType("Sdn Bhd");
        testCaseDTO.setRegistrationNumber("123456789012");
        testCaseDTO.setMerchantCategory("Retail");
        testCaseDTO.setBusinessAddress("No. 123, Jalan Kaya, 50200 Kuala Lumpur");
        testCaseDTO.setDirectorName("John Doe");
        testCaseDTO.setDirectorIC("900101141234");
        testCaseDTO.setDirectorPhone("0121234567");
        testCaseDTO.setDirectorEmail("john@gmail.com");
        testCaseDTO.setAssignedTo("Sarah Lee");
    }

    // ─── getAllCases() ──────────────────────────────────────

    // Test: when a status filter (e.g. "Pending Review") is provided, only cases with that status are returned (paginated)
    @Test
    void getAllCases_WithStatus() {
        Page<OnboardingCase> page = new PageImpl<>(List.of(testCase));
        when(caseRepository.findByStatusOrderByCreatedAtDesc(eq("Pending Review"), any(Pageable.class)))
                .thenReturn(page);

        Page<CaseDTO> result = caseService.getAllCases(0, 10, "Pending Review");

        assertEquals(1, result.getTotalElements());
        assertEquals("ABC Trading Sdn Bhd", result.getContent().get(0).getBusinessName());
    }

    // Test: when no status filter is provided (null), all cases are returned regardless of status
    @Test
    void getAllCases_NoStatus() {
        Page<OnboardingCase> page = new PageImpl<>(List.of(testCase));
        when(caseRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<CaseDTO> result = caseService.getAllCases(0, 10, null);

        assertEquals(1, result.getTotalElements());
    }

    // Test: getAllCasesAsList returns all cases as a sorted flat list (used for dropdowns or exports, not paginated)
    @Test
    void getAllCasesAsList_ReturnsSortedList() {
        when(caseRepository.findAll(any(Sort.class))).thenReturn(List.of(testCase));

        List<CaseDTO> result = caseService.getAllCasesAsList();

        assertEquals(1, result.size());
        assertEquals("MOP-2026-001", result.get(0).getCaseId());
    }

    // ─── createCase() ──────────────────────────────────────

    // Test: creating a new case saves it to the database and returns the correct business name
    @Test
    void createCase_Success() {
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> {
            OnboardingCase c = inv.getArgument(0);
            if (c.getCaseId() == null) c.setCaseId("MOP-2026-001");
            return c;
        });
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        CaseDTO result = caseService.createCase(testCaseDTO);

        assertNotNull(result);
        assertEquals("ABC Trading Sdn Bhd", result.getBusinessName());
        verify(caseRepository).save(any(OnboardingCase.class));
    }

    // Test: when no caseId is provided, the system auto-generates one in the format "MOP-2026-XXX"
    @Test
    void createCase_GeneratesCaseId() {
        testCaseDTO.setCaseId(null);
        when(caseRepository.findAll()).thenReturn(Collections.emptyList());
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        CaseDTO result = caseService.createCase(testCaseDTO);

        assertNotNull(result.getCaseId());
        assertTrue(result.getCaseId().startsWith("MOP-2026-"));
    }

    // ─── saveDraft() ──────────────────────────────────────

    // Test: saving a draft sets the status to "Draft" and does NOT send any notifications (drafts are private)
    @Test
    void saveDraft_Success() {
        when(caseRepository.findAll()).thenReturn(Collections.emptyList());
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));

        CaseDTO result = caseService.saveDraft(testCaseDTO);

        assertNotNull(result);
        assertEquals("Draft", result.getStatus());
        verify(notificationService, never()).notifyCaseCreated(any(), any(), any(), any());
    }

    // ─── updateDraft() ──────────────────────────────────────

    // Test: updating a draft successfully changes its data (e.g. business name)
    @Test
    void updateDraft_Success() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));

        testCaseDTO.setBusinessName("Updated Business Name");
        CaseDTO result = caseService.updateDraft("MOP-2026-001", testCaseDTO);

        assertNotNull(result);
        assertEquals("Updated Business Name", result.getBusinessName());
    }

    // Test: updating a draft that doesn't exist throws ResourceNotFoundException (404)
    @Test
    void updateDraft_CaseNotFound() {
        when(caseRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> caseService.updateDraft("NONEXISTENT", testCaseDTO));
    }

    // ─── getCaseById() ──────────────────────────────────────

    // Test: retrieving a case by its ID returns the correct case data
    @Test
    void getCaseById_Found() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));

        CaseDTO result = caseService.getCaseById("MOP-2026-001");

        assertNotNull(result);
        assertEquals("MOP-2026-001", result.getCaseId());
        assertEquals("ABC Trading Sdn Bhd", result.getBusinessName());
    }

    // Test: retrieving a non-existent case throws ResourceNotFoundException (404)
    @Test
    void getCaseById_NotFound() {
        when(caseRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> caseService.getCaseById("NONEXISTENT"));
    }

    // ─── updateCase() ──────────────────────────────────────

    // Test: updating a case successfully saves changes and adds a history entry to track the status change
    @Test
    void updateCase_Success() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));

        testCaseDTO.setStatus("Background Verification");
        CaseDTO result = caseService.updateCase("MOP-2026-001", testCaseDTO);

        assertNotNull(result);
        // History should include status change entry
        assertFalse(result.getHistory().isEmpty());
    }

    // Test: editing a case that has been "Rejected" is blocked — rejected cases are final and cannot be modified
    @Test
    void updateCase_PreventEditRejected() {
        testCase.setStatus("Rejected");
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));

        assertThrows(IllegalStateException.class,
                () -> caseService.updateCase("MOP-2026-001", testCaseDTO));
    }

    // Test: editing a case that has been "Approved" is blocked — approved cases are final and cannot be modified
    @Test
    void updateCase_PreventEditApproved() {
        testCase.setStatus("Approved");
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));

        assertThrows(IllegalStateException.class,
                () -> caseService.updateCase("MOP-2026-001", testCaseDTO));
    }

    // ─── deleteCase() ──────────────────────────────────────

    // Test: deleting an existing case succeeds and calls deleteById on the repository
    @Test
    void deleteCase_Success() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));

        assertDoesNotThrow(() -> caseService.deleteCase("MOP-2026-001"));
        verify(caseRepository).deleteById("MOP-2026-001");
    }

    // Test: deleting a non-existent case throws ResourceNotFoundException (404)
    @Test
    void deleteCase_NotFound() {
        when(caseRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> caseService.deleteCase("NONEXISTENT"));
    }

    // ─── updateCaseStatus() ──────────────────────────────────

    // Test: updating case status (e.g. to "Background Verification") changes the status field successfully
    @Test
    void updateCaseStatus_Success() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        CaseDTO result = caseService.updateCaseStatus("MOP-2026-001", "Background Verification");

        assertEquals("Background Verification", result.getStatus());
    }

    // Test: when a case is rejected, the system records which stage it was rejected at (e.g. "Compliance Review")
    @Test
    void updateCaseStatus_Rejected_TracksStage() {
        testCase.setStatus("Compliance Review");
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        CaseDTO result = caseService.updateCaseStatus("MOP-2026-001", "Rejected");

        assertEquals("Rejected", result.getStatus());
        assertEquals("Compliance Review", result.getRejectedAtStage());
    }

    // ─── assignCase() ──────────────────────────────────────

    // Test: assigning a case to a reviewer updates the assignedTo field and sends a notification to the assignee
    @Test
    void assignCase_Success() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));
        User assignee = new User();
        assignee.setId("USR002");
        assignee.setName("Jane Smith");
        when(userRepository.findAll()).thenReturn(List.of(assignee));

        CaseDTO result = caseService.assignCase("MOP-2026-001", "Jane Smith");

        assertEquals("Jane Smith", result.getAssignedTo());
        verify(notificationService).notifyCaseAssigned(eq("MOP-2026-001"), eq("ABC Trading Sdn Bhd"),
                eq("USR002"), isNull());
    }

    // ─── uploadDocuments() ──────────────────────────────────

    // Test: uploading a valid PDF file to a case succeeds and returns the updated case
    @Test
    void uploadDocuments_ValidFiles() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "business_cert.pdf", "application/pdf", "PDF content".getBytes());
        String[] types = {"Business Registration Certificate"};

        CaseDTO result = caseService.uploadDocuments("MOP-2026-001",
                new org.springframework.web.multipart.MultipartFile[]{file}, types);

        assertNotNull(result);
    }

    // Test: uploading a dangerous file type (.exe) is rejected — only safe file types (PDF, images) are allowed (security)
    @Test
    void uploadDocuments_InvalidFileType() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));

        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "content".getBytes());
        String[] types = {"Other"};

        assertThrows(IllegalArgumentException.class,
                () -> caseService.uploadDocuments("MOP-2026-001",
                        new org.springframework.web.multipart.MultipartFile[]{file}, types));
    }

    // ─── filterCases() & searchCases() ──────────────────────

    // Test: filtering cases by both status and search keyword returns matching cases
    @Test
    void filterCases_ByStatusAndSearch() {
        testCase.setStatus("pending_review");
        when(caseRepository.searchCases("ABC")).thenReturn(List.of(testCase));

        List<CaseDTO> result = caseService.filterCases("pending_review", "ABC");

        assertEquals(1, result.size());
    }

    // Test: searching cases by keyword (e.g. "ABC") returns cases where the business name matches
    @Test
    void searchCases_ByKeyword() {
        when(caseRepository.searchCases("ABC")).thenReturn(List.of(testCase));

        List<CaseDTO> result = caseService.searchCases("ABC");

        assertEquals(1, result.size());
        assertEquals("ABC Trading Sdn Bhd", result.get(0).getBusinessName());
    }

    // ─── addHistoryEntry() ──────────────────────────────────

    // Test: adding a history entry (e.g. "Comment added") to a case saves it successfully for audit trail
    @Test
    void addHistoryEntry_Success() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any(OnboardingCase.class))).thenReturn(testCase);

        assertDoesNotThrow(() -> caseService.addHistoryEntry("MOP-2026-001", "Comment added"));
        verify(caseRepository).save(any(OnboardingCase.class));
    }
}
