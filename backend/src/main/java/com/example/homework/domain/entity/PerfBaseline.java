package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("perf_baseline")
public class PerfBaseline {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String baseUrl;
    private String path;
    private Integer requests;
    private Integer success;
    private Integer failed;
    private BigDecimal errorRate;
    private BigDecimal minMs;
    private BigDecimal avgMs;
    private BigDecimal p95Ms;
    private BigDecimal maxMs;
    private LocalDateTime generatedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
}

