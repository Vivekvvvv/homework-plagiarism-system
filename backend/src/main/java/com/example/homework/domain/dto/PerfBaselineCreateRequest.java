package com.example.homework.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PerfBaselineCreateRequest {

    @NotBlank(message = "baseUrl cannot be empty")
    private String baseUrl;

    @NotBlank(message = "path cannot be empty")
    private String path;

    @NotNull(message = "requests cannot be null")
    @Min(value = 0, message = "requests must be >= 0")
    private Integer requests;

    @NotNull(message = "success cannot be null")
    @Min(value = 0, message = "success must be >= 0")
    private Integer success;

    @NotNull(message = "failed cannot be null")
    @Min(value = 0, message = "failed must be >= 0")
    private Integer failed;

    @NotNull(message = "errorRate cannot be null")
    @DecimalMin(value = "0.0000", message = "errorRate must be >= 0")
    private BigDecimal errorRate;

    @NotNull(message = "minMs cannot be null")
    @DecimalMin(value = "0.00", message = "minMs must be >= 0")
    private BigDecimal minMs;

    @NotNull(message = "avgMs cannot be null")
    @DecimalMin(value = "0.00", message = "avgMs must be >= 0")
    private BigDecimal avgMs;

    @NotNull(message = "p95Ms cannot be null")
    @DecimalMin(value = "0.00", message = "p95Ms must be >= 0")
    private BigDecimal p95Ms;

    @NotNull(message = "maxMs cannot be null")
    @DecimalMin(value = "0.00", message = "maxMs must be >= 0")
    private BigDecimal maxMs;

    private LocalDateTime generatedAt;
}

