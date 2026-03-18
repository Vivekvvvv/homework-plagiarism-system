package com.example.homework.integration;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.controller.SubmissionController;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionText;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.SubmissionEvolutionPointView;
import com.example.homework.domain.vo.SubmissionView;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerWebMvcTest {

    @Mock
    private SubmissionService submissionService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SubmissionController(submissionService, authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @Test
    void createShouldReturnCreatedSubmission() throws Exception {
        SysUser student = studentUser();
        Submission submission = new Submission();
        submission.setId(31L);
        submission.setAssignmentId(8L);
        submission.setStudentId(3L);
        submission.setVersionNo(2);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(student);
        when(submissionService.create(any(), eq(student))).thenReturn(submission);

        mockMvc.perform(post("/api/v1/submissions")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assignmentId": 8,
                      "studentId": 3,
                      "rawText": "demo content"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(31))
            .andExpect(jsonPath("$.data.assignmentId").value(8))
            .andExpect(jsonPath("$.data.studentId").value(3))
            .andExpect(jsonPath("$.data.versionNo").value(2));
    }

    @Test
    void createShouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/submissions")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rawText": "demo content"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("请求参数有误"));
    }

    @Test
    void createShouldRejectBlankTextWhenNoFile() throws Exception {
        mockMvc.perform(post("/api/v1/submissions")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assignmentId": 8,
                      "studentId": 3,
                      "rawText": "   "
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("请求参数有误"));
    }

    @Test
    void createShouldRejectNonPositiveIds() throws Exception {
        mockMvc.perform(post("/api/v1/submissions")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assignmentId": 0,
                      "studentId": 1,
                      "rawText": "demo content"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("请求参数有误"));
    }

    @Test
    void listByAssignmentShouldReturnSubmissionViews() throws Exception {
        SysUser student = studentUser();
        SubmissionView view = new SubmissionView();
        view.setId(41L);
        view.setAssignmentId(9L);
        view.setStudentId(3L);
        view.setVersionNo(1);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(student);
        when(submissionService.listByAssignmentId(9L, student)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/submissions")
                .principal(authentication())
                .param("assignmentId", "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(41))
            .andExpect(jsonPath("$.data[0].assignmentId").value(9));
    }

    @Test
    void getTextShouldReturnSubmissionText() throws Exception {
        SysUser student = studentUser();
        SubmissionText text = new SubmissionText();
        text.setSubmissionId(41L);
        text.setPlainText("normalized content");
        text.setTokenCount(123);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(student);
        when(submissionService.getTextBySubmissionId(41L, student)).thenReturn(text);

        mockMvc.perform(get("/api/v1/submissions/41/text").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.submissionId").value(41))
            .andExpect(jsonPath("$.data.tokenCount").value(123))
            .andExpect(jsonPath("$.data.plainText").value("normalized content"));
    }

    @Test
    void evolutionShouldReturnTrendPoints() throws Exception {
        SysUser student = studentUser();
        SubmissionEvolutionPointView point = new SubmissionEvolutionPointView();
        point.setSubmissionId(52L);
        point.setVersionNo(3);
        point.setSimilarityToPrevious(new BigDecimal("0.82"));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(student);
        when(submissionService.listEvolution(9L, 3L, student)).thenReturn(List.of(point));

        mockMvc.perform(get("/api/v1/submissions/evolution")
                .principal(authentication())
                .param("assignmentId", "9")
                .param("studentId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].submissionId").value(52))
            .andExpect(jsonPath("$.data[0].versionNo").value(3))
            .andExpect(jsonPath("$.data[0].similarityToPrevious").value(0.82));
    }

    private static SysUser studentUser() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("student1");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private static Authentication authentication() {
        return new TestingAuthenticationToken("student1", null);
    }
}
