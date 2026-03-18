package com.example.homework.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchReviewUpsertRequest {

    @NotNull(message = "reviews cannot be null")
    @Valid
    @Size(min = 1, max = 100, message = "batch size must be between 1 and 100")
    private List<SubmissionReviewUpsertRequest> reviews;
}
