package com.example.homework.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignmentReviewRubricUpsertRequest {

    @NotNull(message = "assignmentId cannot be null")
    private Long assignmentId;

    @NotNull(message = "rubric items cannot be null")
    private List<ReviewRubricItemRequest> items;
}
