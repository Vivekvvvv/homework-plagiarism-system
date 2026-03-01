package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.AuthLoginRequest;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AuthLoginResponse;
import com.example.homework.mapper.SysUserMapper;
import com.example.homework.security.JwtTokenProvider;
import com.example.homework.security.UserRole;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       SysUserMapper sysUserMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserMapper = sysUserMapper;
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
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Unauthorized");
        }
        return getUserByUsername(authentication.getName());
    }

    public SysUser getUserByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username)
            .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "User not found");
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
}
