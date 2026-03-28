package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.dto.BroadcastNotificationRequest;
import com.example.homework.domain.dto.NotificationReadRequest;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.entity.UserNotification;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;
    private final AuthzService authzService;

    public NotificationController(NotificationService notificationService,
                                   AuthService authService,
                                   AuthzService authzService) {
        this.notificationService = notificationService;
        this.authService = authService;
        this.authzService = authzService;
    }

    @GetMapping
    public ApiResponse<List<UserNotification>> list(Authentication authentication,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(defaultValue = "50") Integer limit) {
        SysUser actor = authService.getCurrentUser(authentication);
        return ApiResponse.ok(notificationService.list(actor, status, limit));
    }

    @PostMapping("/read")
    public ApiResponse<Integer> markRead(Authentication authentication,
                                         @Valid @RequestBody NotificationReadRequest request) {
        SysUser actor = authService.getCurrentUser(authentication);
        int updated = notificationService.markRead(actor, Boolean.TRUE.equals(request.getAll()), request.getIds());
        return ApiResponse.ok(updated);
    }

    @PostMapping("/broadcast")
    public ApiResponse<Integer> broadcast(Authentication authentication,
                                          @Valid @RequestBody BroadcastNotificationRequest request) {
        SysUser actor = authService.getCurrentUser(authentication);
        authzService.requireAdminOrTeacher(actor);
        int count = notificationService.broadcast(
                request.getTitle(), request.getContent(),
                request.getLevel(), request.getTarget());
        return ApiResponse.ok(count);
    }
}

