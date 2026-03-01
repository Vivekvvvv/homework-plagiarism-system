package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssignmentStatsView {
    private Long assignmentId;
    private Long courseId;
    private String courseName;
    private String title;
    private LocalDateTime deadline;
    private Integer submissionCount;
    private Integer reviewedCount;
    private BigDecimal averageScore;
    private Integer highRiskPairs;
    private Integer latestTaskStatus;
}

