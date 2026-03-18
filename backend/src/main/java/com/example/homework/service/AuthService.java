package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.AuthLoginRequest;
import com.example.homework.domain.dto.ChangePasswordRequest;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AuthLoginResponse;
import com.example.homework.mapper.SysUserMapper;
import com.example.homework.security.JwtTokenProvider;
import com.example.homework.security.UserRole;
import com.example.homework.util.PasswordValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       SysUserMapper sysUserMapper,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return new AuthLoginResponse(jwtTokenProvider.generateToken(userDetails));
    }

    public SysUser getCurrentUser(String username) {
        SysUser user = getUserByUsername(username);
        user.setPasswordHash(null);
        return user;
    }

    public SysUser getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "未授权，请先登录");
        }
        return getUserByUsername(authentication.getName());
    }

    public SysUser getUserByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username)
            .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public boolean isAdmin(SysUser user) {
        return hasRole(user, UserRole.ADMIN);
    }

    public boolean isTeacher(SysUser user) {
        return hasRole(user, UserRole.TEACHER);
    }

    public boolean isStudent(SysUser user) {
        return hasRole(user, UserRole.STUDENT);
    }

    public boolean hasRole(SysUser user, String role) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().trim().equalsIgnoreCase(role);
    }

    public void changePassword(SysUser currentUser, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "旧密码不正确");
        }
        PasswordValidator.validate(request.getNewPassword());
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "新密码不能与旧密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        auditLogService.log(user, AuditAction.PASSWORD_CHANGE.name(), "user",
            String.valueOf(user.getId()), "密码已修改",
            "/api/v1/auth/change-password", "POST");
    }
}
