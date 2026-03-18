package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.dto.AssignmentCreateRequest;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentMapper assignmentMapper;
    @Mock
    private CourseService courseService;
    @Mock
    private AuthService authService;
    @Mock
    private AuthzService authzService;
    @Mock
    private AuditLogService auditLogService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
            assignmentMapper, courseService, authService, authzService, auditLogService);
    }

    // --- create ---

    @Test
    void createShouldInsertAssignmentForAdmin() {
        SysUser admin = buildUser(1L, "admin", UserRole.ADMIN);
        Course course = buildCourse(10L, 2L);
        when(courseService.getById(10L)).thenReturn(course);
        when(assignmentMapper.insert(any(Assignment.class))).thenReturn(1);

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setCourseId(10L);
        request.setTitle("HW1");
        request.setDescription("First assignment");
        request.setDeadline(LocalDateTime.now().plusDays(7));
        request.setMaxScore(BigDecimal.valueOf(100));

        Assignment result = assignmentService.create(request, admin);

        assertNotNull(result);
        assertEquals("HW1", result.getTitle());
        assertEquals(10L, result.getCourseId());
        assertEquals(2, result.getStatus());
        verify(assignmentMapper).insert(any(Assignment.class));
    }

    @Test
    void createShouldRejectTeacherForOthersCourse() {
        SysUser teacher = buildUser(5L, "teacher1", UserRole.TEACHER);
        Course course = buildCourse(10L, 99L);
        when(courseService.getById(10L)).thenReturn(course);
        when(authService.isTeacher(teacher)).thenReturn(true);

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setCourseId(10L);
        request.setTitle("HW1");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> assignmentService.create(request, teacher));
        assertEquals(403, ex.getCode());
        verify(assignmentMapper, never()).insert(any(Assignment.class));
    }

    // --- listByCourseId ---

    @Test
    @SuppressWarnings("unchecked")
    void listByCourseIdShouldReturnListForCourseOwner() {
        SysUser teacher = buildUser(5L, "teacher1", UserRole.TEACHER);
        Course course = buildCourse(10L, 5L);
        when(courseService.getById(10L)).thenReturn(course);
        when(authService.isTeacher(teacher)).thenReturn(true);

        Assignment a1 = new Assignment();
        a1.setId(1L);
        when(assignmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a1));

        List<Assignment> result = assignmentService.listByCourseId(10L, teacher);

        assertEquals(1, result.size());
    }

    @Test
    void listByCourseIdShouldRejectTeacherForOthersCourse() {
        SysUser teacher = buildUser(5L, "teacher1", UserRole.TEACHER);
        Course course = buildCourse(10L, 99L);
        when(courseService.getById(10L)).thenReturn(course);
        when(authService.isTeacher(teacher)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> assignmentService.listByCourseId(10L, teacher));
        assertEquals(403, ex.getCode());
    }

    // --- getById ---

    @Test
    void getByIdShouldReturnAssignment() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setTitle("HW1");
        when(assignmentMapper.selectById(1L)).thenReturn(assignment);

        Assignment result = assignmentService.getById(1L);

        assertEquals("HW1", result.getTitle());
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(assignmentMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> assignmentService.getById(999L));
        assertEquals(404, ex.getCode());
    }

    // --- getByIdWithAccess ---

    @Test
    void getByIdWithAccessShouldReturnForAdmin() {
        SysUser admin = buildUser(1L, "admin", UserRole.ADMIN);
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setCourseId(10L);
        when(assignmentMapper.selectById(1L)).thenReturn(assignment);
        when(authService.isAdmin(admin)).thenReturn(true);

        Assignment result = assignmentService.getByIdWithAccess(1L, admin);

        assertEquals(1L, result.getId());
    }

    @Test
    void getByIdWithAccessShouldRejectTeacherForOthersCourse() {
        SysUser teacher = buildUser(5L, "teacher1", UserRole.TEACHER);
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setCourseId(10L);
        Course course = buildCourse(10L, 99L);

        when(assignmentMapper.selectById(1L)).thenReturn(assignment);
        when(authService.isAdmin(teacher)).thenReturn(false);
        when(authService.isTeacher(teacher)).thenReturn(true);
        when(courseService.getById(10L)).thenReturn(course);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> assignmentService.getByIdWithAccess(1L, teacher));
        assertEquals(403, ex.getCode());
    }

    // --- helpers ---

    private SysUser buildUser(Long id, String username, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private Course buildCourse(Long id, Long teacherId) {
        Course course = new Course();
        course.setId(id);
        course.setTeacherId(teacherId);
        return course;
    }
}
