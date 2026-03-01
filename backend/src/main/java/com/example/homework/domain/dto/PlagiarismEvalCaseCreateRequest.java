package com.example.homework.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlagiarismEvalCaseCreateRequest {

    @NotBlank(message = "caseName cannot be blank")
    private String caseName;

    @NotBlank(message = "textA cannot be blank")
    private String textA;

    @NotBlank(message = "textB cannot be blank")
    private String textB;

    @NotNull(message = "expectedRiskLevel cannot be null")
    @Min(value = 1, message = "expectedRiskLevel must be between 1 and 3")
    @Max(value = 3, message = "expectedRiskLevel must be between 1 and 3")
    private Integer expectedRiskLevel;

    private String note;
}

