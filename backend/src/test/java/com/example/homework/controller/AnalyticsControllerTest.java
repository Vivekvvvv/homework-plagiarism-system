package com.example.homework.controller;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AnalyticsOverviewView;
import com.example.homework.domain.vo.SubmissionTrendPointView;
import com.example.homework.domain.vo.SystemMetricsView;
import com.example.homework.security.UserRole;
import com.example.homework.service.AnalyticsService;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.SystemMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private SystemMetricsService systemMetricsService;

    @Mock
    private AuthService authService;

    @Mock
    private AuthzService authzService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(analyticsService, systemMetricsService, authService, authzService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void overviewShouldReturnEmptyStats() throws Exception {
        SysUser teacher = teacherUser();
        AnalyticsOverviewView overview = new AnalyticsOverviewView();
        overview.setCourseStats(List.of());
        overview.setAssignmentStats(List.of());
        overview.setStudentStats(List.of());

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(analyticsService.buildOverview(teacher)).thenReturn(overview);

        mockMvc.perform(get("/api/v1/analytics/overview").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.courseStats").isArray())
            .andExpect(jsonPath("$.data.courseStats").isEmpty())
            .andExpect(jsonPath("$.data.assignmentStats").isEmpty())
            .andExpect(jsonPath("$.data.studentStats").isEmpty());

        verify(analyticsService).buildOverview(teacher);
    }

    @Test
    void systemMetricsShouldReturnSnapshot() throws Exception {
        SysUser teacher = teacherUser();
        SystemMetricsView metrics = new SystemMetricsView();
        metrics.setUserCount(100);
        metrics.setTeacherCount(10);
        metrics.setStudentCount(90);
        metrics.setCourseCount(5);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(systemMetricsService.snapshot()).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/analytics/system-metrics").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.userCount").value(100))
            .andExpect(jsonPath("$.data.teacherCount").value(10))
            .andExpect(jsonPath("$.data.studentCount").value(90))
            .andExpect(jsonPath("$.data.courseCount").value(5));

        verify(systemMetricsService).snapshot();
    }

    @Test
    void submissionTrendShouldReturnPointsWithDefaultDays() throws Exception {
        SysUser teacher = teacherUser();
        SubmissionTrendPointView point = new SubmissionTrendPointView();
        point.setDate("2026-03-15");
        point.setCount(12);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(analyticsService.submissionTrend(teacher, 7)).thenReturn(List.of(point));

        mockMvc.perform(get("/api/v1/analytics/submission-trend").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].date").value("2026-03-15"))
            .andExpect(jsonPath("$.data[0].count").value(12));

        verify(analyticsService).submissionTrend(teacher, 7);
    }

    @Test
    void submissionTrendShouldAcceptCustomDays() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(analyticsService.submissionTrend(teacher, 30)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analytics/submission-trend")
                .principal(authentication())
                .param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray());

        verify(analyticsService).submissionTrend(teacher, 30);
    }

    private static SysUser teacherUser() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("teacher1");
        user.setRole(UserRole.TEACHER);
        return user;
    }

    private static Authentication authentication() {
        return new TestingAuthenticationToken("teacher1", null);
    }
}
