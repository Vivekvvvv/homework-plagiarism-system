package com.example.homework.domain.vo;

import lombok.Data;

@Data
public class ReviewSuggestionView {

    private java.math.BigDecimal score;
    private String level;
    private String suggestion;
}
