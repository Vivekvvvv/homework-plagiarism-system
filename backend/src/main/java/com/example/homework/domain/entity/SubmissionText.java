package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_text")
public class SubmissionText {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private String plainText;
    private Integer tokenCount;
    private String preprocessVersion;
    private LocalDateTime createdAt;
}
