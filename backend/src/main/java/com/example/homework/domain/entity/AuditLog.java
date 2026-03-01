package com.example.homework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String requestPath;
    private String requestMethod;
    private LocalDateTime createdAt;
}

