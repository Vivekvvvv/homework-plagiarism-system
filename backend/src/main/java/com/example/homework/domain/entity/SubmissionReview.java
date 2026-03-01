package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("submission_review")
public class SubmissionReview {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long assignmentId;
    private Long reviewerId;
    private BigDecimal score;
    private String comment;
    private String autoComment;
    private String dimensionScoresJson;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
