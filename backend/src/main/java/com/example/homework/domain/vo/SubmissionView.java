package com.example.homework.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionView {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private LocalDateTime submitTime;
    private Integer versionNo;
    private Integer sourceType;
    private String contentHash;
    private Integer tokenCount;
}
