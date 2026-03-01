package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubmissionEvolutionPointView {
    private Long submissionId;
    private Long assignmentId;
    private Long studentId;
    private Integer versionNo;
    private Integer tokenCount;
    private BigDecimal similarityToPrevious;
    private BigDecimal changeRate;
    private Long minutesSincePrevious;
    private BigDecimal score;
    private LocalDateTime reviewedAt;
    private LocalDateTime submitTime;
}
