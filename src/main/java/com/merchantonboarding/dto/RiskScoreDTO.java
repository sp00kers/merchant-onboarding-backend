package com.merchantonboarding.dto;

import java.util.List;

import lombok.Data;

@Data
public class RiskScoreDTO {
    private int totalScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private String recommendation; // AUTO_APPROVE, MANUAL_REVIEW, ENHANCED_DUE_DILIGENCE, REJECTION_RECOMMENDED
    private List<RiskFactor> factors;
    private String calculatedAt;

    @Data
    public static class RiskFactor {
        private String name;
        private String category;
        private int score;
        private int maxScore;
        private int weight;
        private String description;
        private String impact; // POSITIVE, NEGATIVE, NEUTRAL

        public RiskFactor() {}

        public RiskFactor(String name, String category, int score, int maxScore, int weight, String description, String impact) {
            this.name = name;
            this.category = category;
            this.score = score;
            this.maxScore = maxScore;
            this.weight = weight;
            this.description = description;
            this.impact = impact;
        }
    }
}
