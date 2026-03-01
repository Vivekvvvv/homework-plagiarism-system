package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlagiarismReportPairView {
    private Long id;
    private Long submissionAId;
    private Long submissionBId;
    private BigDecimal similarity;
    private Integer riskLevel;
    private Integer hammingDistance;
}
