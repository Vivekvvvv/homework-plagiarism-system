package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlagiarismEvalReportView {
    private Integer totalCases;
    private Integer evaluatedCases;
    private BigDecimal accuracy;
    private BigDecimal macroPrecision;
    private BigDecimal macroRecall;
    private BigDecimal macroF1;
    private List<PerRiskMetric> perRiskMetrics;
    private List<ConfusionRow> confusionMatrix;

    @Data
    public static class PerRiskMetric {
        private Integer riskLevel;
        private BigDecimal precision;
        private BigDecimal recall;
        private BigDecimal f1;
    }

    @Data
    public static class ConfusionRow {
        private Integer expectedRiskLevel;
        private Integer predictedLow;
        private Integer predictedMedium;
        private Integer predictedHigh;
    }
}

