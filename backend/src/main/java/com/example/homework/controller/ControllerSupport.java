package com.example.homework.controller;

import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import org.springframework.security.core.Authentication;

public final class ControllerSupport {

    private ControllerSupport() {
    }

    public static SysUser currentUser(AuthService authService, Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }

    public static SysUser requireTeacherOrAdmin(AuthService authService,
                                                AuthzService authzService,
                                                Authentication authentication) {
        SysUser user = authService.getCurrentUser(authentication);
        authzService.requireRoleIn(user, UserRole.ADMIN, UserRole.TEACHER);
        return user;
    }
}
