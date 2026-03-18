package com.example.homework.service;

import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzServiceTest {

    @Mock
    private AuthService authService;

    private AuthzService authzService;

    @BeforeEach
    void setUp() {
        authzService = new AuthzService(authService);
    }

    // --- requireAdminOrTeacher ---

    @Test
    void requireAdminOrTeacherShouldPassForAdmin() {
        SysUser user = buildUser(1L, UserRole.ADMIN);
        when(authService.isAdmin(user)).thenReturn(true);

        assertDoesNotThrow(() -> authzService.requireAdminOrTeacher(user));
    }

    @Test
    void requireAdminOrTeacherShouldPassForTeacher() {
        SysUser user = buildUser(2L, UserRole.TEACHER);
        when(authService.isAdmin(user)).thenReturn(false);
        when(authService.isTeacher(user)).thenReturn(true);

        assertDoesNotThrow(() -> authzService.requireAdminOrTeacher(user));
    }

    @Test
    void requireAdminOrTeacherShouldRejectStudent() {
        SysUser user = buildUser(3L, UserRole.STUDENT);
        when(authService.isAdmin(user)).thenReturn(false);
        when(authService.isTeacher(user)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authzService.requireAdminOrTeacher(user));
        assertEquals(403, ex.getCode());
    }

    // --- requireAdmin ---

    @Test
    void requireAdminShouldRejectTeacher() {
        SysUser user = buildUser(2L, UserRole.TEACHER);
        when(authService.isAdmin(user)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authzService.requireAdmin(user));
        assertEquals(403, ex.getCode());
    }

    // --- requireStudentSelfOrPrivileged ---

    @Test
    void requireStudentSelfOrPrivilegedShouldPassForAdmin() {
        SysUser user = buildUser(1L, UserRole.ADMIN);
        when(authService.isAdmin(user)).thenReturn(true);

        assertDoesNotThrow(() -> authzService.requireStudentSelfOrPrivileged(user, 99L));
    }

    @Test
    void requireStudentSelfOrPrivilegedShouldPassForSelf() {
        SysUser user = buildUser(5L, UserRole.STUDENT);
        when(authService.isAdmin(user)).thenReturn(false);
        when(authService.isTeacher(user)).thenReturn(false);
        when(authService.isStudent(user)).thenReturn(true);

        assertDoesNotThrow(() -> authzService.requireStudentSelfOrPrivileged(user, 5L));
    }

    @Test
    void requireStudentSelfOrPrivilegedShouldRejectOtherStudent() {
        SysUser user = buildUser(5L, UserRole.STUDENT);
        when(authService.isAdmin(user)).thenReturn(false);
        when(authService.isTeacher(user)).thenReturn(false);
        when(authService.isStudent(user)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authzService.requireStudentSelfOrPrivileged(user, 99L));
        assertEquals(403, ex.getCode());
    }

    // --- requireRoleIn ---

    @Test
    void requireRoleInShouldPassWhenRoleMatches() {
        SysUser user = buildUser(1L, UserRole.ADMIN);
        when(authService.hasRole(user, UserRole.ADMIN)).thenReturn(true);

        assertDoesNotThrow(() -> authzService.requireRoleIn(user, UserRole.ADMIN, UserRole.TEACHER));
    }

    @Test
    void requireRoleInShouldRejectWhenNoMatch() {
        SysUser user = buildUser(3L, UserRole.STUDENT);
        when(authService.hasRole(user, UserRole.ADMIN)).thenReturn(false);
        when(authService.hasRole(user, UserRole.TEACHER)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authzService.requireRoleIn(user, UserRole.ADMIN, UserRole.TEACHER));
        assertEquals(403, ex.getCode());
    }

    // --- helpers ---

    private SysUser buildUser(Long id, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        return user;
    }
}
