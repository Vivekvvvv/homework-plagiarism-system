package com.example.homework.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentReviewRubricView {

    private Long assignmentId;
    private List<RubricItemView> items;
    private LocalDateTime updatedAt;

    @Data
    public static class RubricItemView {
        private String dimension;
        private java.math.BigDecimal weight;
        private String description;
    }
}
