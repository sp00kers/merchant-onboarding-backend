package com.merchantonboarding.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "onboarding_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCase {
    @Id
    @Column(name = "case_id", length = 50)
    private String caseId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "business_type")
    private String businessType;
    
    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "merchant_category")
    private String merchantCategory;

    @Column(name = "business_address", length = 500)
    private String businessAddress;

    @Column(name = "director_name")
    private String directorName;

    @Column(name = "director_ic")
    private String directorIC;

    @Column(name = "director_phone")
    private String directorPhone;

    @Column(name = "director_email")
    private String directorEmail;

    @Column(length = 50)
    private String status = "Pending Review";

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "last_updated")
    private String lastUpdated;

    @OneToMany(mappedBy = "onboardingCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "onboardingCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseHistory> history = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.createdDate == null) {
            this.createdDate = java.time.LocalDate.now().toString();
        }
        if (this.lastUpdated == null) {
            this.lastUpdated = LocalDateTime.now().toString().replace("T", " ").substring(0, 16);
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now().toString().replace("T", " ").substring(0, 16);
    }
}
