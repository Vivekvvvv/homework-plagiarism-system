package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plagiarism_eval_case")
public class PlagiarismEvalCase {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String caseName;
    private String textA;
    private String textB;
    private Integer expectedRiskLevel;
    private Integer predictedRiskLevel;
    private BigDecimal simhashSimilarity;
    private BigDecimal jaccardSimilarity;
    private BigDecimal fusedSimilarity;
    private String note;
    private Integer enabled;
    private LocalDateTime evaluatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

