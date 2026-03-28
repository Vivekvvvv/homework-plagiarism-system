package com.example.homework.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.ApiResponse;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.SysUserMapper;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.homework.controller.ControllerSupport.currentUser;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final AuthzService authzService;

    public UserController(SysUserMapper sysUserMapper,
                          AuthService authService,
                          AuthzService authzService) {
        this.sysUserMapper = sysUserMapper;
        this.authService = authService;
        this.authzService = authzService;
    }

    @GetMapping
    public ApiResponse<List<SysUser>> listAll(Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        authzService.requireAdmin(actor);
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getId));
        // 清除密码哈希，不暴露给前端
        users.forEach(u -> u.setPasswordHash(null));
        return ApiResponse.ok(users);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id,
                                          @RequestParam Integer status,
                                          Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        authzService.requireAdmin(actor);
        if (actor.getId().equals(id)) {
            return ApiResponse.fail(com.example.homework.common.exception.ErrorCodes.BAD_REQUEST, "不能修改自己的状态");
        }
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) {
            return ApiResponse.fail(com.example.homework.common.exception.ErrorCodes.NOT_FOUND, "用户不存在");
        }
        target.setStatus(status);
        target.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(target);
        return ApiResponse.ok();
    }
}
