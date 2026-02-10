package com.merchantonboarding.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskCategory {
    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private String name;

    @Column(name = "score_range", length = 20)
    private String scoreRange;

    @Column(length = 500)
    private String description;

    @Column(name = "actions_required", length = 1000)
    private String actionsRequired;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

