package com.example.homework.service;

import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import org.springframework.stereotype.Service;

@Service
public class AuthzService {

    private final AuthService authService;

    public AuthzService(AuthService authService) {
        this.authService = authService;
    }

    public void requireAdminOrTeacher(SysUser user) {
        if (!(authService.isAdmin(user) || authService.isTeacher(user))) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "需要管理员或教师角色");
        }
    }

    public void requireAdmin(SysUser user) {
        if (!authService.isAdmin(user)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "需要管理员角色");
        }
    }

    public void requireTeacher(SysUser user) {
        if (!authService.isTeacher(user)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "需要教师角色");
        }
    }

    public void requireStudentSelfOrPrivileged(SysUser user, Long targetUserId) {
        if (authService.isAdmin(user) || authService.isTeacher(user)) {
            return;
        }
        if (!authService.isStudent(user)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "需要学生角色");
        }
        if (targetUserId == null || !targetUserId.equals(user.getId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "学生只能访问自己的数据");
        }
    }

    public void requireRoleIn(SysUser user, String... roles) {
        for (String role : roles) {
            if (authService.hasRole(user, role)) {
                return;
            }
        }
        throw new BusinessException(ErrorCodes.FORBIDDEN, "角色权限不足");
    }
}
