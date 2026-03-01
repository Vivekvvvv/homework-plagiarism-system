package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.CourseCreateRequest;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.CourseMapper;
import com.example.homework.security.UserRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final AuthService authService;
    private final AuthzService authzService;
    private final AuditLogService auditLogService;

    public CourseService(CourseMapper courseMapper,
                         AuthService authService,
                         AuthzService authzService,
                         AuditLogService auditLogService) {
        this.courseMapper = courseMapper;
        this.authService = authService;
        this.authzService = authzService;
        this.auditLogService = auditLogService;
    }

    public Course create(CourseCreateRequest request, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        if (authService.isTeacher(actor) && !actor.getId().equals(request.getTeacherId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Teacher can only create course for self");
        }

        Course course = new Course();
        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setTeacherId(request.getTeacherId());
        course.setSemester(request.getSemester());
        course.setStatus(1);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(course);

        auditLogService.log(actor, AuditAction.COURSE_CREATE.name(), "course",
            String.valueOf(course.getId()), course.getCourseCode(),
            "/api/v1/courses", "POST");
        return course;
    }

    public List<Course> listAll(SysUser actor) {
        LambdaQueryWrapper<Course> query = new LambdaQueryWrapper<Course>();
        if (authService.isTeacher(actor)) {
            query.eq(Course::getTeacherId, actor.getId());
        }
        query.orderByDesc(Course::getId);
        return courseMapper.selectList(query);
    }

    public Course getById(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Course not found");
        }
        return course;
    }
}
