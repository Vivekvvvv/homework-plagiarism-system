package com.example.homework.controller;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.domain.entity.PerfBaseline;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.PerfBaselineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerfBaselineControllerTest {

    @Mock
    private PerfBaselineService perfBaselineService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new PerfBaselineController(perfBaselineService, authService, authzService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void createShouldReturnSavedBaseline() throws Exception {
        SysUser teacher = teacherUser();
        PerfBaseline baseline = new PerfBaseline();
        baseline.setId(1L);
        baseline.setBaseUrl("http://localhost:8080");
        baseline.setPath("/api/v1/courses");
        baseline.setRequests(1000);
        baseline.setSuccess(990);
        baseline.setFailed(10);
        baseline.setErrorRate(new BigDecimal("1.0000"));
        baseline.setMinMs(new BigDecimal("5.00"));
        baseline.setAvgMs(new BigDecimal("50.00"));
        baseline.setP95Ms(new BigDecimal("120.00"));
        baseline.setMaxMs(new BigDecimal("500.00"));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(perfBaselineService.create(any(), eq(teacher))).thenReturn(baseline);

        mockMvc.perform(post("/api/v1/perf/baselines")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "baseUrl": "http://localhost:8080",
                      "path": "/api/v1/courses",
                      "requests": 1000,
                      "success": 990,
                      "failed": 10,
                      "errorRate": 1.0000,
                      "minMs": 5.00,
                      "avgMs": 50.00,
                      "p95Ms": 120.00,
                      "maxMs": 500.00
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.baseUrl").value("http://localhost:8080"))
            .andExpect(jsonPath("$.data.path").value("/api/v1/courses"))
            .andExpect(jsonPath("$.data.requests").value(1000))
            .andExpect(jsonPath("$.data.success").value(990))
            .andExpect(jsonPath("$.data.failed").value(10));

        verify(perfBaselineService).create(any(), eq(teacher));
    }

    @Test
    void createShouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/perf/baselines")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "baseUrl": "",
                      "path": ""
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(perfBaselineService);
    }

    @Test
    void listShouldReturnBaselinesWithDefaultLimit() throws Exception {
        SysUser teacher = teacherUser();
        PerfBaseline baseline = new PerfBaseline();
        baseline.setId(1L);
        baseline.setBaseUrl("http://localhost:8080");
        baseline.setPath("/api/v1/courses");
        baseline.setRequests(500);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(perfBaselineService.list(50)).thenReturn(List.of(baseline));

        mockMvc.perform(get("/api/v1/perf/baselines").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].baseUrl").value("http://localhost:8080"))
            .andExpect(jsonPath("$.data[0].requests").value(500));

        verify(perfBaselineService).list(50);
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
