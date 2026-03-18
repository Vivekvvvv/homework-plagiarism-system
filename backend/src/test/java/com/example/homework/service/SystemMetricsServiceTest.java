package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.mapper.CourseMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import com.example.homework.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemMetricsServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private SubmissionReviewMapper submissionReviewMapper;
    @Mock private PlagiarismTaskMapper plagiarismTaskMapper;

    private SystemMetricsService systemMetricsService;

    @BeforeEach
    void setUp() {
        systemMetricsService = new SystemMetricsService(
            sysUserMapper, courseMapper, assignmentMapper,
            submissionMapper, submissionReviewMapper, plagiarismTaskMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void snapshotShouldReturnAllMetrics() {
        when(sysUserMapper.selectCount(isNull())).thenReturn(10L);
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(courseMapper.selectCount(isNull())).thenReturn(5L);
        when(assignmentMapper.selectCount(isNull())).thenReturn(8L);
        when(submissionMapper.selectCount(isNull())).thenReturn(50L);
        when(submissionReviewMapper.selectCount(isNull())).thenReturn(30L);
        when(plagiarismTaskMapper.selectCount(isNull())).thenReturn(4L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(submissionReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(plagiarismTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var result = systemMetricsService.snapshot();

        assertNotNull(result);
        assertEquals(10, result.getUserCount());
        assertEquals(5, result.getCourseCount());
        assertEquals(8, result.getAssignmentCount());
        assertEquals(50, result.getSubmissionCount());
        assertEquals(30, result.getReviewCount());
        assertEquals(4, result.getPlagiarismTaskCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void snapshotShouldHandleEmptyData() {
        when(sysUserMapper.selectCount(isNull())).thenReturn(0L);
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(courseMapper.selectCount(isNull())).thenReturn(0L);
        when(assignmentMapper.selectCount(isNull())).thenReturn(0L);
        when(submissionMapper.selectCount(isNull())).thenReturn(0L);
        when(submissionReviewMapper.selectCount(isNull())).thenReturn(0L);
        when(plagiarismTaskMapper.selectCount(isNull())).thenReturn(0L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(submissionReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(plagiarismTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var result = systemMetricsService.snapshot();

        assertNotNull(result);
        assertEquals(0, result.getUserCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void snapshotShouldSumHighRiskPairs() {
        when(sysUserMapper.selectCount(isNull())).thenReturn(1L);
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(courseMapper.selectCount(isNull())).thenReturn(1L);
        when(assignmentMapper.selectCount(isNull())).thenReturn(1L);
        when(submissionMapper.selectCount(isNull())).thenReturn(1L);
        when(submissionReviewMapper.selectCount(isNull())).thenReturn(0L);
        when(plagiarismTaskMapper.selectCount(isNull())).thenReturn(2L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(submissionReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(plagiarismTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        PlagiarismTask task1 = new PlagiarismTask();
        task1.setHighRiskPairs(5);
        PlagiarismTask task2 = new PlagiarismTask();
        task2.setHighRiskPairs(3);
        when(plagiarismTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task1, task2));

        var result = systemMetricsService.snapshot();

        assertEquals(8, result.getPlagiarismHighRiskPairs());
    }
}
