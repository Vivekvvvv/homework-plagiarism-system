package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plagiarism_pair_result")
public class PlagiarismPairResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long submissionAId;
    private Long submissionBId;
    private String pairKey;
    private BigDecimal similarity;
    private BigDecimal simhashSimilarity;
    private BigDecimal jaccardSimilarity;
    private Integer hammingDistance;
    private Integer riskLevel;
    private String matchedFragmentsJson;
    private String explainJson;
    private LocalDateTime createdAt;
}
