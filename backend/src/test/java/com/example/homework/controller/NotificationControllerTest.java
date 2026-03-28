package com.example.homework.controller;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.entity.UserNotification;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService, authService, authzService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void listShouldReturnNotificationsWithDefaultParams() throws Exception {
        SysUser teacher = teacherUser();
        UserNotification notification = new UserNotification();
        notification.setId(1L);
        notification.setUserId(2L);
        notification.setTitle("New submission");
        notification.setContent("Student submitted homework");
        notification.setLevel("info");
        notification.setStatus(0);
        notification.setCreatedAt(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(notificationService.list(teacher, null, 50)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/v1/notifications").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].title").value("New submission"))
            .andExpect(jsonPath("$.data[0].level").value("info"))
            .andExpect(jsonPath("$.data[0].status").value(0));

        verify(notificationService).list(teacher, null, 50);
    }

    @Test
    void listShouldFilterByStatusParam() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(notificationService.list(teacher, 0, 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications")
                .principal(authentication())
                .param("status", "0")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray());

        verify(notificationService).list(teacher, 0, 20);
    }

    @Test
    void markReadShouldMarkAllAsRead() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(notificationService.markRead(eq(teacher), eq(true), any())).thenReturn(5);

        mockMvc.perform(post("/api/v1/notifications/read")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "all": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(5));

        verify(notificationService).markRead(eq(teacher), eq(true), any());
    }

    @Test
    void markReadShouldRejectNullAll() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "ids": [1, 2]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
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
