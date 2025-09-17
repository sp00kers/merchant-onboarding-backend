package com.merchantonboarding.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private OnboardingCase onboardingCase;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    private String comments;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum Status {
        SUBMITTED,
        UNDER_REVIEW,
        COMPLIANCE_CHECK,
        VERIFICATION_PENDING,
        APPROVED,
        REJECTED,
        ADDITIONAL_INFO_REQUIRED
    }
}
