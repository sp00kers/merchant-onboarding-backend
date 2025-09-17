package com.merchantonboarding.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "onboarding_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "merchant_name", nullable = false)
    private String merchantName;
    
    @Column(name = "business_type")
    private String businessType;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    private String email;
    private String address;
    
    @Enumerated(EnumType.STRING)
    private CaseStatus status = CaseStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "onboardingCase", cascade = CascadeType.ALL)
    private List<Document> documents;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum CaseStatus {
        PENDING, IN_REVIEW, APPROVED, REJECTED, VERIFICATION_REQUIRED
    }
}
