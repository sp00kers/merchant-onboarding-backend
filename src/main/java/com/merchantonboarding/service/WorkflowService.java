package com.merchantonboarding.service;

import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.WorkflowStatus;
import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.CaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkflowService {

    @Autowired
    private CaseRepository caseRepository;

    /**
     * Progress case to next workflow stage
     */
    public void progressCase(Long caseId, OnboardingCase.CaseStatus newStatus, User updatedBy, String comments) {
        OnboardingCase onboardingCase = caseRepository.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found"));

        // Update case status
        onboardingCase.setStatus(newStatus);
        
        // Create workflow status entry
        WorkflowStatus workflowStatus = new WorkflowStatus();
        workflowStatus.setOnboardingCase(onboardingCase);
        workflowStatus.setStatus(mapCaseStatusToWorkflowStatus(newStatus));
        workflowStatus.setComments(comments);
        workflowStatus.setUpdatedBy(updatedBy);

        caseRepository.save(onboardingCase);
    }

    /**
     * Auto-assign case based on workload
     */
    public void autoAssignCase(Long caseId) {
        // Implementation for auto-assignment logic
        // This could be based on current workload, expertise, etc.
    }

    private WorkflowStatus.Status mapCaseStatusToWorkflowStatus(OnboardingCase.CaseStatus caseStatus) {
        return switch (caseStatus) {
            case PENDING -> WorkflowStatus.Status.SUBMITTED;
            case IN_REVIEW -> WorkflowStatus.Status.UNDER_REVIEW;
            case VERIFICATION_REQUIRED -> WorkflowStatus.Status.VERIFICATION_PENDING;
            case APPROVED -> WorkflowStatus.Status.APPROVED;
            case REJECTED -> WorkflowStatus.Status.REJECTED;
        };
    }
}
