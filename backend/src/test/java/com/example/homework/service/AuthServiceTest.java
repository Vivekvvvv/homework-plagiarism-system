package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.dto.AuthLoginRequest;
import com.example.homework.domain.dto.ChangePasswordRequest;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AuthLoginResponse;
import com.example.homework.mapper.SysUserMapper;
import com.example.homework.security.JwtTokenProvider;
import com.example.homework.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            authenticationManager, jwtTokenProvider, sysUserMapper,
            passwordEncoder, auditLogService);
    }

    // --- login ---

    @Test
    void loginShouldReturnTokenOnSuccess() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("admin");
        request.setPassword("pass");

        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = new User("admin", "pass", List.of());
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");

        AuthLoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void loginShouldThrowOnBadCredentials() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    // --- getCurrentUser ---

    @Test
    @SuppressWarnings("unchecked")
    void getCurrentUserByUsernameShouldReturnUserWithNullPassword() {
        SysUser user = buildUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("hashed");
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        SysUser result = authService.getCurrentUser("admin");

        assertNull(result.getPasswordHash());
        assertEquals("admin", result.getUsername());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCurrentUserByUsernameShouldThrowWhenNotFound() {
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser("unknown"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getCurrentUserByAuthShouldThrowWhenAuthNull() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser((Authentication) null));
        assertEquals(401, ex.getCode());
    }

    // --- role checks ---

    @Test
    void isAdminShouldReturnTrueForAdmin() {
        SysUser user = buildUser(1L, "admin", UserRole.ADMIN);
        assertTrue(authService.isAdmin(user));
    }

    @Test
    void isAdminShouldReturnFalseForStudent() {
        SysUser user = buildUser(1L, "student", UserRole.STUDENT);
        assertFalse(authService.isAdmin(user));
    }

    @Test
    void hasRoleShouldBeCaseInsensitive() {
        SysUser user = buildUser(1L, "admin", "Admin");
        assertTrue(authService.hasRole(user, "ADMIN"));
    }

    @Test
    void hasRoleShouldReturnFalseForNullUser() {
        assertFalse(authService.hasRole(null, "ADMIN"));
    }

    // --- changePassword ---

    @Test
    void changePasswordShouldSucceed() {
        SysUser user = buildUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("old-hash");
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("OldPass1!", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass1@", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass1@")).thenReturn("new-hash");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass1!");
        request.setNewPassword("NewPass1@");

        authService.changePassword(user, request);

        verify(sysUserMapper).updateById(any(SysUser.class));
        verify(auditLogService).log(any(), eq("PASSWORD_CHANGE"), eq("user"),
            any(), eq("密码已修改"), any(), eq("POST"));
    }

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        SysUser user = buildUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("old-hash");
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("NewPass1@");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.changePassword(user, request));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("旧密码不正确"));
    }

    @Test
    void changePasswordShouldRejectWeakNewPassword() {
        SysUser user = buildUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("old-hash");
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("OldPass1!", "old-hash")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass1!");
        request.setNewPassword("weak");

        assertThrows(BusinessException.class,
            () -> authService.changePassword(user, request));
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void changePasswordShouldRejectSamePassword() {
        SysUser user = buildUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("old-hash");
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("OldPass1!", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("OldPass1!", "old-hash")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass1!");
        request.setNewPassword("OldPass1!");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.changePassword(user, request));
        assertTrue(ex.getMessage().contains("新密码不能与旧密码相同"));
    }

    // --- helpers ---

    private SysUser buildUser(Long id, String username, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
