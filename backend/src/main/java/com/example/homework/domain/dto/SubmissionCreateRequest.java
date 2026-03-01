package com.example.homework.domain.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionCreateRequest {

    @NotNull(message = "作业ID不能为空")
    @Min(value = 1, message = "作业ID必须为正数")
    private Long assignmentId;

    @NotNull(message = "学生ID不能为空")
    @Min(value = 1, message = "学生ID必须为正数")
    private Long studentId;

    private Long fileId;

    private String rawText;

    @AssertTrue(message = "提交内容不能为空")
    public boolean isContentProvided() {
        if (fileId != null) {
            return true;
        }
        return rawText != null && !rawText.trim().isEmpty();
    }
}
