package com.example.homework.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class NotificationReadRequest {

    private List<Long> ids;

    @NotNull(message = "all cannot be null")
    private Boolean all;
}

