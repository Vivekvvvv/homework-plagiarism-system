package com.example.homework.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemMetricsView {
    private Integer userCount;
    private Integer teacherCount;
    private Integer studentCount;
    private Integer courseCount;
    private Integer assignmentCount;
    private Integer submissionCount;
    private Integer reviewCount;
    private Integer plagiarismTaskCount;
    private Integer plagiarismHighRiskPairs;
    private LocalDateTime latestSubmissionTime;
    private LocalDateTime latestReviewTime;
    private LocalDateTime latestPlagiarismTaskTime;
}

