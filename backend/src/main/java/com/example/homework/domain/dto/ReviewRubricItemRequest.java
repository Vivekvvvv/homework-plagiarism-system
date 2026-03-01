package com.example.homework.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewRubricItemRequest {

    @NotBlank(message = "dimension name cannot be blank")
    private String dimension;

    @DecimalMin(value = "0.0", message = "weight cannot be lower than 0")
    @DecimalMax(value = "100.0", message = "weight cannot be higher than 100")
    private BigDecimal weight;

    private String description;
}
