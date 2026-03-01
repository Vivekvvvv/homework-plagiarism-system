package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlagiarismTaskReportView {
    private Long assignmentId;
    private Long taskId;
    private Integer status;
    private String algorithm;
    private BigDecimal threshold;
    private BigDecimal simhashWeight;
    private BigDecimal jaccardWeight;
    private Integer totalPairs;
    private Integer highRiskPairs;
    private Integer retryCount;
    private Integer maxRetry;
    private Integer runTimeoutSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long lowRiskCount;
    private Long mediumRiskCount;
    private Long highRiskCount;
    private List<PlagiarismReportPairView> topPairs;
}
