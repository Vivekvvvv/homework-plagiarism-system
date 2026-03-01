package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plagiarism_task")
public class PlagiarismTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private String algorithm;
    private Integer status;
    private BigDecimal threshold;
    private BigDecimal simhashWeight;
    private BigDecimal jaccardWeight;
    private Integer totalPairs;
    private Integer highRiskPairs;
    private String errorMessage;
    private String idempotencyKey;
    private Integer retryCount;
    private Integer maxRetry;
    private Integer runTimeoutSeconds;
    private Long createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
