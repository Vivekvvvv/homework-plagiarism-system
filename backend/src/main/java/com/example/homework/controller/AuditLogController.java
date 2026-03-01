package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.entity.AuditLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuditLogService;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final AuthzService authzService;

    public AuditLogController(AuditLogService auditLogService,
                              AuthService authService,
                              AuthzService authzService) {
        this.auditLogService = auditLogService;
        this.authService = authService;
        this.authzService = authzService;
    }

    @GetMapping("/logs")
    public ApiResponse<List<AuditLog>> listLogs(Authentication authentication,
                                                @RequestParam(required = false) String actorUsername,
                                                @RequestParam(required = false) String action,
                                                @RequestParam(defaultValue = "100") Integer limit) {
        SysUser actor = authService.getCurrentUser(authentication);
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        return ApiResponse.ok(auditLogService.list(actorUsername, action, limit));
    }
}

