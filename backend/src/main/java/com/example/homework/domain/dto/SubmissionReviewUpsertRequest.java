package com.example.homework.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SubmissionReviewUpsertRequest {

    @NotNull(message = "submissionId cannot be null")
    private Long submissionId;

    @DecimalMin(value = "0.00", message = "score cannot be lower than 0")
    @DecimalMax(value = "100.00", message = "score cannot be higher than 100")
    private BigDecimal score;

    @Valid
    private List<ReviewDimensionScoreRequest> dimensionScores;

    private String comment;
}
