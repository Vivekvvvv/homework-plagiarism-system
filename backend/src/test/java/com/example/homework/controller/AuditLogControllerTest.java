package com.example.homework.controller;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.domain.entity.AuditLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuditLogService;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
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

import java.time.LocalDateTime;
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
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(auditLogService, authService, authzService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void listLogsShouldReturnAllWithNoFilters() throws Exception {
        SysUser teacher = teacherUser();
        AuditLog log1 = new AuditLog();
        log1.setId(1L);
        log1.setActorUsername("teacher1");
        log1.setActorRole(UserRole.TEACHER);
        log1.setAction("LOGIN");
        log1.setRequestPath("/api/v1/auth/login");
        log1.setRequestMethod("POST");
        log1.setCreatedAt(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(auditLogService.list(null, null, 100)).thenReturn(List.of(log1));

        mockMvc.perform(get("/api/v1/audit/logs").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("teacher1"))
            .andExpect(jsonPath("$.data[0].action").value("LOGIN"));

        verify(auditLogService).list(null, null, 100);
    }

    @Test
    void listLogsShouldFilterByActorUsername() throws Exception {
        SysUser teacher = teacherUser();
        AuditLog log1 = new AuditLog();
        log1.setId(2L);
        log1.setActorUsername("student1");
        log1.setAction("FILE_UPLOAD");

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(auditLogService.list("student1", null, 100)).thenReturn(List.of(log1));

        mockMvc.perform(get("/api/v1/audit/logs")
                .principal(authentication())
                .param("actorUsername", "student1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(2))
            .andExpect(jsonPath("$.data[0].actorUsername").value("student1"))
            .andExpect(jsonPath("$.data[0].action").value("FILE_UPLOAD"));

        verify(auditLogService).list("student1", null, 100);
    }

    @Test
    void listLogsShouldFilterByAction() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(auditLogService.list(null, "LOGIN", 50)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/audit/logs")
                .principal(authentication())
                .param("action", "LOGIN")
                .param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());

        verify(auditLogService).list(null, "LOGIN", 50);
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
