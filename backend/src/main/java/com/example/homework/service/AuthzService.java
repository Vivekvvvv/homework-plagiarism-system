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
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Admin or teacher role required");
        }
    }

    public void requireAdmin(SysUser user) {
        if (!authService.isAdmin(user)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Admin role required");
        }
    }

    public void requireTeacher(SysUser user) {
        if (!authService.isTeacher(user)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Teacher role required");
        }
    }

    public void requireStudentSelfOrPrivileged(SysUser user, Long targetUserId) {
        if (authService.isAdmin(user) || authService.isTeacher(user)) {
            return;
        }
        if (!authService.isStudent(user)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Student role required");
        }
        if (targetUserId == null || !targetUserId.equals(user.getId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Students can only access their own data");
        }
    }

    public void requireRoleIn(SysUser user, String... roles) {
        for (String role : roles) {
            if (authService.hasRole(user, role)) {
                return;
            }
        }
        throw new BusinessException(ErrorCodes.FORBIDDEN, "Role not allowed");
    }
}
