package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AnalyticsOverviewView;
import com.example.homework.domain.vo.SubmissionTrendPointView;
import com.example.homework.domain.vo.SystemMetricsView;
import com.example.homework.service.AnalyticsService;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.SystemMetricsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.homework.controller.ControllerSupport.requireTeacherOrAdmin;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SystemMetricsService systemMetricsService;
    private final AuthService authService;
    private final AuthzService authzService;

    public AnalyticsController(AnalyticsService analyticsService,
                               SystemMetricsService systemMetricsService,
                               AuthService authService,
                               AuthzService authzService) {
        this.analyticsService = analyticsService;
        this.systemMetricsService = systemMetricsService;
        this.authService = authService;
        this.authzService = authzService;
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverviewView> overview(Authentication authentication) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(analyticsService.buildOverview(actor));
    }

    @GetMapping("/system-metrics")
    public ApiResponse<SystemMetricsView> systemMetrics(Authentication authentication) {
        requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(systemMetricsService.snapshot());
    }

    @GetMapping("/submission-trend")
    public ApiResponse<List<SubmissionTrendPointView>> submissionTrend(
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(analyticsService.submissionTrend(actor, days));
    }
}

