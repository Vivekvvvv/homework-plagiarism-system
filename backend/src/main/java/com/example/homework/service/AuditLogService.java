package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.AuditLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void log(SysUser actor,
                    String action,
                    String targetType,
                    String targetId,
                    String detail,
                    String requestPath,
                    String requestMethod) {
        AuditLog log = new AuditLog();
        log.setActorUsername(actor == null ? "anonymous" : actor.getUsername());
        log.setActorRole(actor == null ? null : actor.getRole());
        log.setAction(shortText(action, 64));
        log.setTargetType(shortText(targetType, 64));
        log.setTargetId(shortText(targetId, 64));
        log.setDetail(shortText(detail, 1000));
        log.setRequestPath(shortText(requestPath, 255));
        log.setRequestMethod(shortText(requestMethod, 16));
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    public List<AuditLog> list(String actorUsername, String action, Integer limit) {
        int safeLimit = com.example.homework.common.QueryHelper.safeLimit(limit, 100, 500);
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<AuditLog>();
        if (StringUtils.hasText(actorUsername)) {
            query.eq(AuditLog::getActorUsername, actorUsername.trim());
        }
        if (StringUtils.hasText(action)) {
            query.eq(AuditLog::getAction, action.trim());
        }
        query.orderByDesc(AuditLog::getId).last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(query);
    }

    private String shortText(String input, int limit) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String cleaned = input.replaceAll("\\s+", " ").trim();
        return cleaned.length() > limit ? cleaned.substring(0, limit) : cleaned;
    }
}

