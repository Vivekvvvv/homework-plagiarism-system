package com.example.homework.integration;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.controller.ReviewController;
import com.example.homework.domain.entity.SubmissionReview;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.ReviewSuggestionView;
import com.example.homework.domain.vo.SubmissionReviewSummaryView;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.ReviewService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerWebMvcTest {

    @Mock
    private ReviewService reviewService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService, authService, authzService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void upsertShouldReturnSavedReview() throws Exception {
        SysUser teacher = teacherUser();
        SubmissionReview review = new SubmissionReview();
        review.setId(71L);
        review.setSubmissionId(41L);
        review.setScore(new BigDecimal("92.50"));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(reviewService.upsertReview(any(), eq(teacher))).thenReturn(review);

        mockMvc.perform(post("/api/v1/reviews")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "submissionId": 41,
                      "score": 92.5,
                      "comment": "well done"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(71))
            .andExpect(jsonPath("$.data.submissionId").value(41))
            .andExpect(jsonPath("$.data.score").value(92.5));
    }

    @Test
    void summaryShouldReturnAggregatedMetrics() throws Exception {
        SysUser teacher = teacherUser();
        SubmissionReviewSummaryView summary = new SubmissionReviewSummaryView();
        summary.setTotalSubmissions(20);
        summary.setReviewedSubmissions(12);
        summary.setReviewedRate(new BigDecimal("60.00"));
        summary.setAverageScore(new BigDecimal("85.50"));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(reviewService.summaryByAssignmentId(9L, teacher)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reviews/summary")
                .principal(authentication())
                .param("assignmentId", "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.totalSubmissions").value(20))
            .andExpect(jsonPath("$.data.reviewedSubmissions").value(12))
            .andExpect(jsonPath("$.data.averageScore").value(85.5));
    }

    @Test
    void suggestionShouldUseDefaultScoreWhenParamMissing() throws Exception {
        SysUser teacher = teacherUser();
        ReviewSuggestionView suggestion = new ReviewSuggestionView();
        suggestion.setScore(new BigDecimal("60"));
        suggestion.setLevel("PASS");
        suggestion.setSuggestion("keep improving");

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(reviewService.buildSuggestion(new BigDecimal("60"))).thenReturn(suggestion);

        mockMvc.perform(get("/api/v1/reviews/suggestion").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.score").value(60))
            .andExpect(jsonPath("$.data.level").value("PASS"));
    }

    @Test
    void exportShouldReturnCsvBytes() throws Exception {
        SysUser teacher = teacherUser();
        byte[] csv = "submissionId,score\n41,92.5\n".getBytes(StandardCharsets.UTF_8);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        doNothing().when(authzService).requireRoleIn(any(SysUser.class), any(String[].class));
        when(reviewService.exportReviewsCsv(9L, teacher)).thenReturn(csv);

        mockMvc.perform(get("/api/v1/reviews/export")
                .principal(authentication())
                .param("assignmentId", "9"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
            .andExpect(content().bytes(csv));
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
