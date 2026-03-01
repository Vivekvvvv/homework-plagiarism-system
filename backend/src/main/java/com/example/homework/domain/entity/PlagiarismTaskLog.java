package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plagiarism_task_log")
public class PlagiarismTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String phase;
    private String message;
    private LocalDateTime createdAt;
}
