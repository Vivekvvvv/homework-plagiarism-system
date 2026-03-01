package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmissionReviewSummaryView {
    private Integer totalSubmissions;
    private Integer reviewedSubmissions;
    private BigDecimal reviewedRate;
    private BigDecimal averageScore;
    private Integer passCount;
    private BigDecimal passRate;
}
