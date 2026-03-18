package com.example.homework.controller;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.domain.entity.PlagiarismEvalCase;
import com.example.homework.domain.entity.PlagiarismEvalRun;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.PlagiarismEvalReportView;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuditLogService;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.PlagiarismEvalService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlagiarismEvalControllerTest {

    @Mock
    private PlagiarismEvalService plagiarismEvalService;

    @Mock
    private AuthService authService;

    @Mock
    private AuthzService authzService;

    @Mock
    private AuditLogService auditLogService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PlagiarismEvalController(plagiarismEvalService, authService, authzService, auditLogService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void createCaseShouldReturnCreatedCase() throws Exception {
        SysUser teacher = teacherUser();
        PlagiarismEvalCase evalCase = new PlagiarismEvalCase();
        evalCase.setId(1L);
        evalCase.setCaseName("test-case-1");
        evalCase.setTextA("sample text A");
        evalCase.setTextB("sample text B");
        evalCase.setExpectedRiskLevel(2);
        evalCase.setEnabled(1);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(plagiarismEvalService.createCase(any())).thenReturn(evalCase);

        mockMvc.perform(post("/api/v1/plagiarism/evaluation/cases")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "caseName": "test-case-1",
                      "textA": "sample text A",
                      "textB": "sample text B",
                      "expectedRiskLevel": 2,
                      "note": "test note"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.caseName").value("test-case-1"))
            .andExpect(jsonPath("$.data.expectedRiskLevel").value(2))
            .andExpect(jsonPath("$.data.enabled").value(1));

        verify(plagiarismEvalService).createCase(any());
        verify(auditLogService).log(eq(teacher), eq("EVAL_CASE_CREATE"), eq("plagiarism_eval_case"),
            eq("1"), eq("test-case-1"), eq("/api/v1/plagiarism/evaluation/cases"), eq("POST"));
    }

    @Test
    void listCasesShouldReturnAllCases() throws Exception {
        SysUser teacher = teacherUser();
        PlagiarismEvalCase evalCase = new PlagiarismEvalCase();
        evalCase.setId(1L);
        evalCase.setCaseName("case-1");
        evalCase.setEnabled(1);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(plagiarismEvalService.listCases(null)).thenReturn(List.of(evalCase));

        mockMvc.perform(get("/api/v1/plagiarism/evaluation/cases").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].caseName").value("case-1"));

        verify(plagiarismEvalService).listCases(null);
    }

    @Test
    void runShouldReturnEvaluatedCases() throws Exception {
        SysUser teacher = teacherUser();
        PlagiarismEvalCase evalCase = new PlagiarismEvalCase();
        evalCase.setId(1L);
        evalCase.setCaseName("case-1");
        evalCase.setPredictedRiskLevel(2);
        evalCase.setFusedSimilarity(new BigDecimal("0.7500"));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(plagiarismEvalService.runEvaluation(any(), any(), any())).thenReturn(List.of(evalCase));

        mockMvc.perform(post("/api/v1/plagiarism/evaluation/run").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].predictedRiskLevel").value(2));

        verify(plagiarismEvalService).runEvaluation(any(), any(), any());
        verify(plagiarismEvalService).recordRun(any(), any(), any(), eq(2L));
    }

    @Test
    void reportShouldReturnEvalReport() throws Exception {
        SysUser teacher = teacherUser();
        PlagiarismEvalReportView report = new PlagiarismEvalReportView();
        report.setTotalCases(10);
        report.setEvaluatedCases(8);
        report.setAccuracy(new BigDecimal("0.8750"));
        report.setMacroPrecision(new BigDecimal("0.8000"));
        report.setMacroRecall(new BigDecimal("0.7500"));
        report.setMacroF1(new BigDecimal("0.7742"));
        report.setPerRiskMetrics(List.of());
        report.setConfusionMatrix(List.of());

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(plagiarismEvalService.report()).thenReturn(report);

        mockMvc.perform(get("/api/v1/plagiarism/evaluation/report").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.totalCases").value(10))
            .andExpect(jsonPath("$.data.evaluatedCases").value(8))
            .andExpect(jsonPath("$.data.accuracy").value(0.8750))
            .andExpect(jsonPath("$.data.perRiskMetrics").isArray())
            .andExpect(jsonPath("$.data.confusionMatrix").isArray());

        verify(plagiarismEvalService).report();
    }

    @Test
    void runsShouldReturnRunHistory() throws Exception {
        SysUser teacher = teacherUser();
        PlagiarismEvalRun run = new PlagiarismEvalRun();
        run.setId(1L);
        run.setThreshold(new BigDecimal("0.7000"));
        run.setSimhashWeight(new BigDecimal("0.7000"));
        run.setJaccardWeight(new BigDecimal("0.3000"));
        run.setTotalCases(10);
        run.setEvaluatedCases(8);
        run.setAccuracy(new BigDecimal("0.8750"));
        run.setRunBy(2L);
        run.setCreatedAt(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(teacher, UserRole.ADMIN, UserRole.TEACHER);
        when(plagiarismEvalService.listRuns(20)).thenReturn(List.of(run));

        mockMvc.perform(get("/api/v1/plagiarism/evaluation/runs").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].totalCases").value(10))
            .andExpect(jsonPath("$.data[0].accuracy").value(0.8750))
            .andExpect(jsonPath("$.data[0].runBy").value(2));

        verify(plagiarismEvalService).listRuns(20);
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
