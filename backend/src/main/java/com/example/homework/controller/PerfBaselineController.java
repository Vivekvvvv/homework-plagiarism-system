package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.dto.PerfBaselineCreateRequest;
import com.example.homework.domain.entity.PerfBaseline;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.PerfBaselineService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.homework.controller.ControllerSupport.requireTeacherOrAdmin;

@RestController
@RequestMapping("/api/v1/perf")
public class PerfBaselineController {

    private final PerfBaselineService perfBaselineService;
    private final AuthService authService;
    private final AuthzService authzService;

    public PerfBaselineController(PerfBaselineService perfBaselineService,
                                  AuthService authService,
                                  AuthzService authzService) {
        this.perfBaselineService = perfBaselineService;
        this.authService = authService;
        this.authzService = authzService;
    }

    @PostMapping("/baselines")
    public ApiResponse<PerfBaseline> create(Authentication authentication,
                                            @Valid @RequestBody PerfBaselineCreateRequest request) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(perfBaselineService.create(request, actor));
    }

    @GetMapping("/baselines")
    public ApiResponse<List<PerfBaseline>> list(Authentication authentication,
                                                @RequestParam(defaultValue = "50") Integer limit) {
        requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(perfBaselineService.list(limit));
    }
}

