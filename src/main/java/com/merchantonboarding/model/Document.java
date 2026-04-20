package com.merchantonboarding.model;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "uploaded_at")
    private String uploadedAt;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data")
    private byte[] fileData;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OnboardingCase onboardingCase;
    
    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now().toString().replace("T", " ").substring(0, 16);
        }
    }
}
