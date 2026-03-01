package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlagiarismTaskTrendPointView {
    private Long taskId;
    private Integer status;
    private BigDecimal threshold;
    private Integer totalPairs;
    private Integer highRiskPairs;
    private BigDecimal highRiskRate;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
