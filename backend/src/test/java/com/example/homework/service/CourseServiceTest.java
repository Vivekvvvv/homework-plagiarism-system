package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.dto.CourseCreateRequest;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.CourseMapper;
import com.example.homework.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private AuthService authService;
    @Mock
    private AuthzService authzService;
    @Mock
    private AuditLogService auditLogService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseMapper, authService, authzService, auditLogService);
    }

    // --- create ---

    @Test
    void createShouldInsertCourseForAdmin() {
        SysUser admin = buildUser(1L, "admin", UserRole.ADMIN);
        CourseCreateRequest request = new CourseCreateRequest();
        request.setCourseCode("CS101");
        request.setCourseName("Data Structures");
        request.setTeacherId(2L);
        request.setSemester("2026-Spring");

        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        Course result = courseService.create(request, admin);

        assertNotNull(result);
        assertEquals("CS101", result.getCourseCode());
        assertEquals("Data Structures", result.getCourseName());
        assertEquals(2L, result.getTeacherId());
        assertEquals(1, result.getStatus());
        verify(authzService).requireRoleIn(admin, UserRole.ADMIN, UserRole.TEACHER);
        verify(courseMapper).insert(any(Course.class));
        verify(auditLogService).log(eq(admin), eq("COURSE_CREATE"), eq("course"),
            any(), eq("CS101"), eq("/api/v1/courses"), eq("POST"));
    }

    @Test
    void createShouldRejectTeacherCreatingForOthers() {
        SysUser teacher = buildUser(10L, "teacher1", UserRole.TEACHER);
        CourseCreateRequest request = new CourseCreateRequest();
        request.setCourseCode("CS102");
        request.setCourseName("Algorithms");
        request.setTeacherId(99L);
        request.setSemester("2026-Spring");

        when(authService.isTeacher(teacher)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> courseService.create(request, teacher));
        assertEquals(403, ex.getCode());
        verify(courseMapper, never()).insert(any(Course.class));
    }

    @Test
    void createShouldAllowTeacherCreatingForSelf() {
        SysUser teacher = buildUser(10L, "teacher1", UserRole.TEACHER);
        CourseCreateRequest request = new CourseCreateRequest();
        request.setCourseCode("CS103");
        request.setCourseName("OS");
        request.setTeacherId(10L);
        request.setSemester("2026-Spring");

        when(authService.isTeacher(teacher)).thenReturn(true);
        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        Course result = courseService.create(request, teacher);

        assertNotNull(result);
        assertEquals("CS103", result.getCourseCode());
        verify(courseMapper).insert(any(Course.class));
    }

    // --- listAll ---

    @Test
    @SuppressWarnings("unchecked")
    void listAllShouldReturnAllCoursesForAdmin() {
        SysUser admin = buildUser(1L, "admin", UserRole.ADMIN);
        when(authService.isTeacher(admin)).thenReturn(false);

        Course c1 = new Course();
        c1.setId(1L);
        Course c2 = new Course();
        c2.setId(2L);
        when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2));

        List<Course> result = courseService.listAll(admin);

        assertEquals(2, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listAllShouldFilterByTeacherIdForTeacher() {
        SysUser teacher = buildUser(10L, "teacher1", UserRole.TEACHER);
        when(authService.isTeacher(teacher)).thenReturn(true);

        Course c1 = new Course();
        c1.setId(1L);
        when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1));

        List<Course> result = courseService.listAll(teacher);

        assertEquals(1, result.size());
    }

    // --- getById ---

    @Test
    void getByIdShouldReturnCourseWhenExists() {
        Course course = new Course();
        course.setId(1L);
        course.setCourseCode("CS101");
        when(courseMapper.selectById(1L)).thenReturn(course);

        Course result = courseService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("CS101", result.getCourseCode());
    }

    @Test
    void getByIdShouldThrowWhenNotExists() {
        when(courseMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> courseService.getById(999L));
        assertEquals(404, ex.getCode());
    }

    // --- helpers ---

    private SysUser buildUser(Long id, String username, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
