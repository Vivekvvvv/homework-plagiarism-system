package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseStatsView {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String semester;
    private Integer assignmentCount;
    private Integer submissionCount;
    private Integer reviewedCount;
    private BigDecimal averageScore;
    private Integer highRiskAssignments;
}

