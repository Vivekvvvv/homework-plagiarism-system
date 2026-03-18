package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AnalyticsOverviewView;
import com.example.homework.domain.vo.SubmissionTrendPointView;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CourseService courseService;
    @Mock
    private AssignmentMapper assignmentMapper;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private SubmissionReviewMapper submissionReviewMapper;
    @Mock
    private PlagiarismTaskMapper plagiarismTaskMapper;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
            courseService, assignmentMapper, submissionMapper,
            submissionReviewMapper, plagiarismTaskMapper);
    }

    // --- buildOverview ---

    @Test
    void buildOverviewShouldReturnEmptyWhenNoCourses() {
        SysUser actor = buildUser(1L);
        when(courseService.listAll(actor)).thenReturn(List.of());

        AnalyticsOverviewView result = analyticsService.buildOverview(actor);

        assertNotNull(result);
        assertTrue(result.getCourseStats().isEmpty());
        assertTrue(result.getAssignmentStats().isEmpty());
        assertTrue(result.getStudentStats().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildOverviewShouldReturnStatsWhenCoursesExist() {
        SysUser actor = buildUser(1L);
        Course course = new Course();
        course.setId(10L);
        course.setCourseCode("CS101");
        course.setCourseName("Data Structures");
        course.setSemester("2026-Spring");
        when(courseService.listAll(actor)).thenReturn(List.of(course));

        Assignment assignment = new Assignment();
        assignment.setId(100L);
        assignment.setCourseId(10L);
        assignment.setTitle("HW1");
        when(assignmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(assignment));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(plagiarismTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        AnalyticsOverviewView result = analyticsService.buildOverview(actor);

        assertNotNull(result);
        assertEquals(1, result.getCourseStats().size());
        assertEquals("CS101", result.getCourseStats().get(0).getCourseCode());
        assertEquals(1, result.getAssignmentStats().size());
        assertEquals("HW1", result.getAssignmentStats().get(0).getTitle());
        assertTrue(result.getStudentStats().isEmpty());
    }

    // --- submissionTrend ---

    @Test
    void submissionTrendShouldReturnEmptyDaysWhenNoCourses() {
        SysUser actor = buildUser(1L);
        when(courseService.listAll(actor)).thenReturn(List.of());

        List<SubmissionTrendPointView> result = analyticsService.submissionTrend(actor, 7);

        assertEquals(7, result.size());
        // All counts should be 0
        for (SubmissionTrendPointView point : result) {
            assertEquals(0L, point.getCount());
            assertNotNull(point.getDate());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void submissionTrendShouldReturnEmptyDaysWhenNoAssignments() {
        SysUser actor = buildUser(1L);
        Course course = new Course();
        course.setId(10L);
        when(courseService.listAll(actor)).thenReturn(List.of(course));
        when(assignmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<SubmissionTrendPointView> result = analyticsService.submissionTrend(actor, 7);

        assertEquals(7, result.size());
        for (SubmissionTrendPointView point : result) {
            assertEquals(0L, point.getCount());
        }
    }

    @Test
    void submissionTrendShouldClampDaysToValidRange() {
        SysUser actor = buildUser(1L);
        when(courseService.listAll(actor)).thenReturn(List.of());

        // days=0 should become 1
        List<SubmissionTrendPointView> result1 = analyticsService.submissionTrend(actor, 0);
        assertEquals(1, result1.size());

        // days=100 should become 90
        List<SubmissionTrendPointView> result2 = analyticsService.submissionTrend(actor, 100);
        assertEquals(90, result2.size());
    }

    @Test
    void submissionTrendShouldHaveDatesInOrder() {
        SysUser actor = buildUser(1L);
        when(courseService.listAll(actor)).thenReturn(List.of());

        List<SubmissionTrendPointView> result = analyticsService.submissionTrend(actor, 3);

        assertEquals(3, result.size());
        // First date should be 2 days ago, last date should be today
        String today = LocalDate.now().toString();
        String twoDaysAgo = LocalDate.now().minusDays(2).toString();
        assertEquals(twoDaysAgo, result.get(0).getDate());
        assertEquals(today, result.get(2).getDate());
    }

    // --- helpers ---

    private SysUser buildUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        return user;
    }
}
