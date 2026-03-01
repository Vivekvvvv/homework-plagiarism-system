package com.example.homework.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AnalyticsOverviewView {
    private List<CourseStatsView> courseStats;
    private List<AssignmentStatsView> assignmentStats;
    private List<StudentStatsView> studentStats;
}

