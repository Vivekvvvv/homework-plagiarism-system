package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentStatsView {
    private Long studentId;
    private Integer submissionCount;
    private Integer reviewedCount;
    private BigDecimal averageScore;
    private LocalDateTime latestSubmitTime;
}

