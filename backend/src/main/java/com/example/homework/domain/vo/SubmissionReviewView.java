package com.example.homework.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubmissionReviewView {
    private Long submissionId;
    private Long assignmentId;
    private Long studentId;
    private Integer versionNo;
    private Integer sourceType;
    private Integer tokenCount;
    private LocalDateTime submitTime;

    private Long reviewId;
    private Long reviewerId;
    private BigDecimal score;
    private String comment;
    private String autoComment;
    private String dimensionScoresJson;
    private LocalDateTime reviewedAt;
}
