package com.example.homework.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BroadcastNotificationRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200字")
    private String title;

    @Size(max = 1000, message = "内容最多1000字")
    private String content;

    /** info / warning / danger / success */
    private String level = "info";

    /** 目标人群: all / student / teacher */
    private String target = "all";
}
