package com.example.homework.integration;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.controller.CourseController;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuthService;
import com.example.homework.service.CourseService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseControllerWebMvcTest {

    @Mock
    private CourseService courseService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(courseService, authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @Test
    void createShouldReturnCreatedCourse() throws Exception {
        SysUser teacher = teacherUser();
        Course course = new Course();
        course.setId(11L);
        course.setCourseCode("SE101");
        course.setCourseName("Software Engineering");

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(courseService.create(any(), eq(teacher))).thenReturn(course);

        mockMvc.perform(post("/api/v1/courses")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "SE101",
                      "courseName": "Software Engineering",
                      "teacherId": 2,
                      "semester": "2025-2026-2"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(11))
            .andExpect(jsonPath("$.data.courseCode").value("SE101"))
            .andExpect(jsonPath("$.data.courseName").value("Software Engineering"));
    }

    @Test
    void createShouldRejectBlankFields() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "teacherId": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("请求参数有误"));
    }

    @Test
    void listAllShouldReturnCourseList() throws Exception {
        SysUser teacher = teacherUser();
        Course course = new Course();
        course.setId(12L);
        course.setCourseCode("CS102");
        course.setCourseName("Data Structures");

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(courseService.listAll(teacher)).thenReturn(List.of(course));

        mockMvc.perform(get("/api/v1/courses").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].id").value(12))
            .andExpect(jsonPath("$.data[0].courseCode").value("CS102"));
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
