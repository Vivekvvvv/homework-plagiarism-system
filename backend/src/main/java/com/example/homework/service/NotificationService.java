package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.entity.UserNotification;
import com.example.homework.mapper.SysUserMapper;
import com.example.homework.mapper.UserNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final UserNotificationMapper userNotificationMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationService(UserNotificationMapper userNotificationMapper,
                               SysUserMapper sysUserMapper,
                               NotificationWebSocketHandler webSocketHandler) {
        this.userNotificationMapper = userNotificationMapper;
        this.sysUserMapper = sysUserMapper;
        this.webSocketHandler = webSocketHandler;
    }

    public UserNotification createNotification(Long userId,
                                               String title,
                                               String content,
                                               String level,
                                               String sourceType,
                                               String sourceId) {
        if (userId == null) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "userId cannot be null");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "title cannot be empty");
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(shortText(title, 200));
        notification.setContent(shortText(content, 1000));
        notification.setLevel(normalizeLevel(level));
        notification.setStatus(0);
        notification.setSourceType(shortText(sourceType, 64));
        notification.setSourceId(shortText(sourceId, 64));
        notification.setCreatedAt(LocalDateTime.now());
        userNotificationMapper.insert(notification);

        // Push via WebSocket (best-effort, never fails the main flow)
        try {
            SysUser targetUser = sysUserMapper.selectById(userId);
            if (targetUser != null && targetUser.getUsername() != null) {
                webSocketHandler.sendToUser(targetUser.getUsername(), notification);
            }
        } catch (Exception ignored) {
            // WebSocket push failure should not affect notification creation
        }

        return notification;
    }

    public List<UserNotification> list(SysUser actor, Integer status, Integer limit) {
        int safeLimit = com.example.homework.common.QueryHelper.safeLimit(limit, 50, 200);
        LambdaQueryWrapper<UserNotification> query = new LambdaQueryWrapper<UserNotification>()
            .eq(UserNotification::getUserId, actor.getId())
            .orderByDesc(UserNotification::getId)
            .last("LIMIT " + safeLimit);
        if (status != null) {
            query.eq(UserNotification::getStatus, status);
        }
        return userNotificationMapper.selectList(query);
    }

    public int markRead(SysUser actor, boolean all, List<Long> ids) {
        if (!all && (ids == null || ids.isEmpty())) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "ids cannot be empty when all=false");
        }

        UserNotification update = new UserNotification();
        update.setStatus(1);
        update.setReadAt(LocalDateTime.now());

        LambdaQueryWrapper<UserNotification> query = new LambdaQueryWrapper<UserNotification>()
            .eq(UserNotification::getUserId, actor.getId());
        if (!all) {
            query.in(UserNotification::getId, ids);
        }
        return userNotificationMapper.update(update, query);
    }

    private String normalizeLevel(String input) {
        if (!StringUtils.hasText(input)) {
            return "info";
        }
        String cleaned = input.trim().toLowerCase();
        if (cleaned.equals("warning") || cleaned.equals("warn")) {
            return "warning";
        }
        if (cleaned.equals("danger") || cleaned.equals("error")) {
            return "danger";
        }
        if (cleaned.equals("success")) {
            return "success";
        }
        return "info";
    }

    private String shortText(String input, int maxLen) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String cleaned = input.replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }
}

