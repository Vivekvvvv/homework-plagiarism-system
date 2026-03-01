package com.example.homework.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewDimensionScoreRequest {

    @NotBlank(message = "dimension cannot be blank")
    private String dimension;

    @DecimalMin(value = "0.0", message = "dimension score cannot be lower than 0")
    @DecimalMax(value = "100.0", message = "dimension score cannot be higher than 100")
    private BigDecimal score;

    private String comment;
}
