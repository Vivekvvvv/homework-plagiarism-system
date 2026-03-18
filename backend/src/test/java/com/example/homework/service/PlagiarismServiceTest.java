package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.PlagiarismTaskLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.PlagiarismPairResultMapper;
import com.example.homework.mapper.PlagiarismTaskLogMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlagiarismServiceTest {

    @Mock private PlagiarismTaskMapper plagiarismTaskMapper;
    @Mock private PlagiarismPairResultMapper plagiarismPairResultMapper;
    @Mock private PlagiarismTaskLogMapper plagiarismTaskLogMapper;
    @Mock private PlagiarismTaskRunnerService plagiarismTaskRunnerService;
    @Mock private AssignmentService assignmentService;
    @Mock private AuthzService authzService;
    @Mock private AuditLogService auditLogService;

    private PlagiarismService plagiarismService;

    @BeforeEach
    void setUp() {
        plagiarismService = new PlagiarismService(
            plagiarismTaskMapper, plagiarismPairResultMapper, plagiarismTaskLogMapper,
            plagiarismTaskRunnerService, assignmentService, authzService, auditLogService);
    }

    // --- getTaskById ---

    @Test
    @SuppressWarnings("unchecked")
    void getTaskByIdShouldReturnTask() {
        PlagiarismTask task = new PlagiarismTask();
        task.setId(1L);
        task.setAssignmentId(10L);
        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        PlagiarismTask result = plagiarismService.getTaskById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTaskByIdShouldThrowWhenNotFound() {
        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> plagiarismService.getTaskById(999L));
        assertEquals(404, ex.getCode());
    }

    // --- cancelTask ---

    @Test
    @SuppressWarnings("unchecked")
    void cancelTaskShouldSetStatus4() {
        SysUser teacher = buildUser(1L, UserRole.TEACHER);
        PlagiarismTask task = new PlagiarismTask();
        task.setId(1L);
        task.setAssignmentId(10L);
        task.setStatus(1);
        Assignment assignment = new Assignment();
        assignment.setId(10L);

        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        when(assignmentService.getByIdWithAccess(10L, teacher)).thenReturn(assignment);
        when(plagiarismTaskLogMapper.insert(any(PlagiarismTaskLog.class))).thenReturn(1);

        PlagiarismTask result = plagiarismService.cancelTask(1L, teacher);

        assertEquals(4, result.getStatus());
        verify(plagiarismTaskMapper).updateById(task);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancelTaskShouldRejectAlreadyFinished() {
        SysUser teacher = buildUser(1L, UserRole.TEACHER);
        PlagiarismTask task = new PlagiarismTask();
        task.setId(1L);
        task.setAssignmentId(10L);
        task.setStatus(2);
        Assignment assignment = new Assignment();
        assignment.setId(10L);

        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        when(assignmentService.getByIdWithAccess(10L, teacher)).thenReturn(assignment);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> plagiarismService.cancelTask(1L, teacher));
        assertEquals(400, ex.getCode());
    }

    // --- listTasksByAssignment ---

    @Test
    @SuppressWarnings("unchecked")
    void listTasksByAssignmentShouldReturnList() {
        SysUser teacher = buildUser(1L, UserRole.TEACHER);
        Assignment assignment = new Assignment();
        assignment.setId(10L);
        when(assignmentService.getByIdWithAccess(10L, teacher)).thenReturn(assignment);

        PlagiarismTask task = new PlagiarismTask();
        task.setId(1L);
        when(plagiarismTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));

        List<PlagiarismTask> result = plagiarismService.listTasksByAssignment(10L, teacher);

        assertEquals(1, result.size());
    }

    // --- logTask ---

    @Test
    void logTaskShouldInsertLog() {
        when(plagiarismTaskLogMapper.insert(any(PlagiarismTaskLog.class))).thenReturn(1);

        assertDoesNotThrow(() -> plagiarismService.logTask(1L, "INFO", "Task started"));
        verify(plagiarismTaskLogMapper).insert(any(PlagiarismTaskLog.class));
    }

    // --- helpers ---

    private SysUser buildUser(Long id, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        return user;
    }
}
