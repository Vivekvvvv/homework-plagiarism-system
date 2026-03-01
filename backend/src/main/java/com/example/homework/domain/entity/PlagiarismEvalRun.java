package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plagiarism_eval_run")
public class PlagiarismEvalRun {

    @TableId(type = IdType.AUTO)
    private Long id;
    private BigDecimal threshold;
    private BigDecimal simhashWeight;
    private BigDecimal jaccardWeight;
    private Integer totalCases;
    private Integer evaluatedCases;
    private BigDecimal accuracy;
    private BigDecimal macroPrecision;
    private BigDecimal macroRecall;
    private BigDecimal macroF1;
    private Long runBy;
    private LocalDateTime createdAt;
}

