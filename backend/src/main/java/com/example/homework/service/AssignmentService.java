package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.AssignmentCreateRequest;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.security.UserRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final CourseService courseService;
    private final AuthService authService;
    private final AuthzService authzService;
    private final AuditLogService auditLogService;

    public AssignmentService(AssignmentMapper assignmentMapper,
                             CourseService courseService,
                             AuthService authService,
                             AuthzService authzService,
                             AuditLogService auditLogService) {
        this.assignmentMapper = assignmentMapper;
        this.courseService = courseService;
        this.authService = authService;
        this.authzService = authzService;
        this.auditLogService = auditLogService;
    }

    public Assignment create(AssignmentCreateRequest request, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        Course course = courseService.getById(request.getCourseId());
        if (authService.isTeacher(actor) && !actor.getId().equals(course.getTeacherId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Teacher can only create assignment for own course");
        }

        Assignment assignment = new Assignment();
        assignment.setCourseId(request.getCourseId());
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDeadline(request.getDeadline());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setStatus(2);
        assignment.setCreatedBy(actor.getId());
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);

        auditLogService.log(actor, AuditAction.ASSIGNMENT_CREATE.name(), "assignment",
            String.valueOf(assignment.getId()), assignment.getTitle(),
            "/api/v1/assignments", "POST");
        return assignment;
    }

    public List<Assignment> listByCourseId(Long courseId, SysUser actor) {
        Course course = courseService.getById(courseId);
        if (authService.isTeacher(actor) && !actor.getId().equals(course.getTeacherId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Teacher can only view assignments of own course");
        }
        return assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
            .eq(Assignment::getCourseId, courseId)
            .orderByDesc(Assignment::getId));
    }

    public Assignment getById(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Assignment not found");
        }
        return assignment;
    }

    public Assignment getByIdWithAccess(Long assignmentId, SysUser actor) {
        Assignment assignment = getById(assignmentId);
        if (authService.isAdmin(actor)) {
            return assignment;
        }
        if (authService.isTeacher(actor)) {
            Course course = courseService.getById(assignment.getCourseId());
            if (!actor.getId().equals(course.getTeacherId())) {
                throw new BusinessException(ErrorCodes.FORBIDDEN, "Teacher can only access assignments of own course");
            }
            return assignment;
        }
        throw new BusinessException(ErrorCodes.FORBIDDEN, "Only admin or teacher can access assignment details");
    }
}
