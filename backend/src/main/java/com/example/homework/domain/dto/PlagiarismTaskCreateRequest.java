package com.example.homework.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlagiarismTaskCreateRequest {

    @NotNull(message = "assignmentId cannot be null")
    private Long assignmentId;

    @DecimalMin(value = "0.1", message = "threshold cannot be lower than 0.1")
    @DecimalMax(value = "1.0", message = "threshold cannot be higher than 1.0")
    private BigDecimal threshold = BigDecimal.valueOf(0.70);

    @DecimalMin(value = "0.0", message = "simhashWeight cannot be lower than 0")
    @DecimalMax(value = "1.0", message = "simhashWeight cannot be higher than 1")
    private BigDecimal simhashWeight = BigDecimal.valueOf(0.70);

    @DecimalMin(value = "0.0", message = "jaccardWeight cannot be lower than 0")
    @DecimalMax(value = "1.0", message = "jaccardWeight cannot be higher than 1")
    private BigDecimal jaccardWeight = BigDecimal.valueOf(0.30);

    private String idempotencyKey;

    private Integer maxRetry = 1;

    private Integer runTimeoutSeconds = 120;
}
