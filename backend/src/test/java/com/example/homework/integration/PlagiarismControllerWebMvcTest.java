package com.example.homework.integration;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.controller.PlagiarismController;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.entity.PlagiarismPairResult;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.PlagiarismService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlagiarismControllerWebMvcTest {

    @Mock
    private PlagiarismService plagiarismService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new PlagiarismController(plagiarismService, authService, authzService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void createTaskShouldReturnCreatedTaskForTeacher() throws Exception {
        SysUser teacher = teacherUser();
        PlagiarismTask task = new PlagiarismTask();
        task.setId(9L);
        task.setAssignmentId(7L);
        task.setStatus(0);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.createTask(any(), eq(teacher))).thenReturn(task);

        mockMvc.perform(post("/api/v1/plagiarism/tasks")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assignmentId": 7,
                      "threshold": 0.7,
                      "simhashWeight": 0.7,
                      "jaccardWeight": 0.3
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(9))
            .andExpect(jsonPath("$.data.assignmentId").value(7))
            .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void createTaskShouldRejectMissingAssignmentId() throws Exception {
        mockMvc.perform(post("/api/v1/plagiarism/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("Bad request parameters"));
    }

    @Test
    void cancelTaskShouldReturnBusinessError() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.cancelTask(12L, teacher))
            .thenThrow(new BusinessException(400, "Task cannot be canceled in current status"));

        mockMvc.perform(patch("/api/v1/plagiarism/tasks/12/cancel").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("Task cannot be canceled in current status"));
    }

    @Test
    void retryTaskShouldReturnBusinessError() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.retryTask(13L, teacher))
            .thenThrow(new BusinessException(400, "Task is still pending or running"));

        mockMvc.perform(post("/api/v1/plagiarism/tasks/13/retry").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("Task is still pending or running"));
    }

    @Test
    void exportPairsShouldReturnCsvAttachment() throws Exception {
        SysUser teacher = teacherUser();
        byte[] csv = "pairId,similarity\n1,0.91\n".getBytes(StandardCharsets.UTF_8);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.exportPairsCsv(
            eq(12L),
            eq(3),
            any(BigDecimal.class),
            eq(teacher)
        )).thenReturn(csv);

        mockMvc.perform(get("/api/v1/plagiarism/tasks/12/pairs/export")
                .principal(authentication())
                .param("riskLevel", "3")
                .param("minSimilarity", "0.80"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
            .andExpect(content().bytes(csv));
    }

    @Test
    void exportPairsShouldReturnBusinessError() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.exportPairsCsv(eq(404L), eq(null), eq(null), eq(teacher)))
            .thenThrow(new BusinessException(404, "Plagiarism task not found"));

        mockMvc.perform(get("/api/v1/plagiarism/tasks/404/pairs/export")
                .principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.msg").value("Plagiarism task not found"));
    }

    @Test
    void listPairsShouldReturnEmptyPageWhenNoData() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.listPairsByTask(eq(12L), eq(null), eq(null), eq(1L), eq(20L), eq(teacher)))
            .thenReturn(new com.example.homework.common.PageResult<>(0, 1, 20, List.of()));

        mockMvc.perform(get("/api/v1/plagiarism/tasks/12/pairs")
                .principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void pairDetailShouldReturnBusinessError() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.getPairById(404L, teacher))
            .thenThrow(new BusinessException(404, "Pair result not found"));

        mockMvc.perform(get("/api/v1/plagiarism/pairs/404").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.msg").value("Pair result not found"));
    }

    @Test
    void assignmentTrendShouldRejectInvalidRange() throws Exception {
        SysUser teacher = teacherUser();

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(plagiarismService.listAssignmentTrend(eq(9L), any(), any(), eq(20), eq(teacher)))
            .thenThrow(new BusinessException(400, "startAt cannot be later than endAt"));

        mockMvc.perform(get("/api/v1/plagiarism/assignments/9/trend")
                .principal(authentication())
                .param("startAt", "2026-03-10T10:00:00")
                .param("endAt", "2026-03-10T09:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("startAt cannot be later than endAt"));
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
