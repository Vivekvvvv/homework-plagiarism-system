package com.example.homework.integration;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.controller.AssignmentController;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AssignmentService;
import com.example.homework.service.AuthService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerWebMvcTest {

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentController(assignmentService, authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @Test
    void createShouldReturnCreatedAssignment() throws Exception {
        SysUser teacher = teacherUser();
        Assignment assignment = new Assignment();
        assignment.setId(21L);
        assignment.setCourseId(9L);
        assignment.setTitle("Design Doc");
        assignment.setMaxScore(new BigDecimal("100"));
        assignment.setDeadline(LocalDateTime.of(2026, 3, 15, 12, 0));

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(assignmentService.create(any(), eq(teacher))).thenReturn(assignment);

        mockMvc.perform(post("/api/v1/assignments")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseId": 9,
                      "title": "Design Doc",
                      "description": "integration test",
                      "deadline": "2026-03-15T12:00:00",
                      "maxScore": 100,
                      "createdBy": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(21))
            .andExpect(jsonPath("$.data.courseId").value(9))
            .andExpect(jsonPath("$.data.title").value("Design Doc"));
    }

    @Test
    void createShouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/assignments")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Design Doc"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("Bad request parameters"));
    }

    @Test
    void listByCourseShouldReturnAssignments() throws Exception {
        SysUser teacher = teacherUser();
        Assignment assignment = new Assignment();
        assignment.setId(31L);
        assignment.setCourseId(9L);
        assignment.setTitle("Assignment A");

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(assignmentService.listByCourseId(9L, teacher)).thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/v1/assignments")
                .principal(authentication())
                .param("courseId", "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(31))
            .andExpect(jsonPath("$.data[0].courseId").value(9))
            .andExpect(jsonPath("$.data[0].title").value("Assignment A"));
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
