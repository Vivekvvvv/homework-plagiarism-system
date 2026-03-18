package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionReview;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AnalyticsOverviewView;
import com.example.homework.domain.vo.AssignmentStatsView;
import com.example.homework.domain.vo.CourseStatsView;
import com.example.homework.domain.vo.StudentStatsView;
import com.example.homework.domain.vo.SubmissionTrendPointView;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final CourseService courseService;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionReviewMapper submissionReviewMapper;
    private final PlagiarismTaskMapper plagiarismTaskMapper;

    public AnalyticsService(CourseService courseService,
                            AssignmentMapper assignmentMapper,
                            SubmissionMapper submissionMapper,
                            SubmissionReviewMapper submissionReviewMapper,
                            PlagiarismTaskMapper plagiarismTaskMapper) {
        this.courseService = courseService;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.submissionReviewMapper = submissionReviewMapper;
        this.plagiarismTaskMapper = plagiarismTaskMapper;
    }

    public AnalyticsOverviewView buildOverview(SysUser actor) {
        List<Course> courses = courseService.listAll(actor);
        AnalyticsOverviewView overview = new AnalyticsOverviewView();
        if (courses.isEmpty()) {
            overview.setCourseStats(List.of());
            overview.setAssignmentStats(List.of());
            overview.setStudentStats(List.of());
            return overview;
        }

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<Assignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
            .in(Assignment::getCourseId, courseIds)
            .orderByDesc(Assignment::getId));

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        List<Submission> submissions = assignmentIds.isEmpty()
            ? List.of()
            : submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .in(Submission::getAssignmentId, assignmentIds));

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<SubmissionReview> reviews = submissionIds.isEmpty()
            ? List.of()
            : submissionReviewMapper.selectList(new LambdaQueryWrapper<SubmissionReview>()
                .in(SubmissionReview::getSubmissionId, submissionIds));

        Map<Long, SubmissionReview> reviewMap = new HashMap<>();
        for (SubmissionReview review : reviews) {
            reviewMap.put(review.getSubmissionId(), review);
        }

        Map<Long, PlagiarismTask> latestTaskMap = new HashMap<>();
        if (!assignmentIds.isEmpty()) {
            List<PlagiarismTask> tasks = plagiarismTaskMapper.selectList(new LambdaQueryWrapper<PlagiarismTask>()
                .in(PlagiarismTask::getAssignmentId, assignmentIds)
                .orderByDesc(PlagiarismTask::getId));
            for (PlagiarismTask task : tasks) {
                latestTaskMap.putIfAbsent(task.getAssignmentId(), task);
            }
        }

        Map<Long, List<Submission>> submissionsByAssignment = submissions.stream()
            .collect(Collectors.groupingBy(Submission::getAssignmentId));

        Map<Long, List<Submission>> submissionsByStudent = submissions.stream()
            .collect(Collectors.groupingBy(Submission::getStudentId));

        Map<Long, Course> courseMap = courses.stream()
            .collect(Collectors.toMap(Course::getId, course -> course));

        List<AssignmentStatsView> assignmentStats = new ArrayList<>();
        for (Assignment assignment : assignments) {
            List<Submission> assignmentSubmissions = submissionsByAssignment.getOrDefault(assignment.getId(), List.of());
            int submissionCount = assignmentSubmissions.size();
            int reviewedCount = 0;
            BigDecimal scoreSum = BigDecimal.ZERO;
            for (Submission submission : assignmentSubmissions) {
                SubmissionReview review = reviewMap.get(submission.getId());
                if (review != null && review.getScore() != null) {
                    reviewedCount++;
                    scoreSum = scoreSum.add(review.getScore());
                }
            }
            BigDecimal avgScore = reviewedCount > 0
                ? scoreSum.divide(BigDecimal.valueOf(reviewedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            PlagiarismTask latest = latestTaskMap.get(assignment.getId());
            AssignmentStatsView view = new AssignmentStatsView();
            view.setAssignmentId(assignment.getId());
            view.setCourseId(assignment.getCourseId());
            Course course = courseMap.get(assignment.getCourseId());
            view.setCourseName(course == null ? null : course.getCourseName());
            view.setTitle(assignment.getTitle());
            view.setDeadline(assignment.getDeadline());
            view.setSubmissionCount(submissionCount);
            view.setReviewedCount(reviewedCount);
            view.setAverageScore(avgScore);
            view.setHighRiskPairs(latest == null ? 0 : safeInt(latest.getHighRiskPairs()));
            view.setLatestTaskStatus(latest == null ? null : latest.getStatus());
            assignmentStats.add(view);
        }

        List<CourseStatsView> courseStats = new ArrayList<>();
        Map<Long, List<AssignmentStatsView>> assignmentStatsByCourse = assignmentStats.stream()
            .collect(Collectors.groupingBy(AssignmentStatsView::getCourseId));
        for (Course course : courses) {
            List<AssignmentStatsView> courseAssignments = assignmentStatsByCourse.getOrDefault(course.getId(), List.of());
            int assignmentCount = courseAssignments.size();
            int submissionCount = courseAssignments.stream().mapToInt(a -> safeInt(a.getSubmissionCount())).sum();
            int reviewedCount = courseAssignments.stream().mapToInt(a -> safeInt(a.getReviewedCount())).sum();
            BigDecimal scoreSum = BigDecimal.ZERO;
            int scoredAssignments = 0;
            int highRiskAssignments = 0;
            for (AssignmentStatsView item : courseAssignments) {
                if (item.getAverageScore() != null && item.getAverageScore().compareTo(BigDecimal.ZERO) > 0) {
                    scoreSum = scoreSum.add(item.getAverageScore());
                    scoredAssignments++;
                }
                if (safeInt(item.getHighRiskPairs()) > 0) {
                    highRiskAssignments++;
                }
            }
            BigDecimal avgScore = scoredAssignments > 0
                ? scoreSum.divide(BigDecimal.valueOf(scoredAssignments), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            CourseStatsView view = new CourseStatsView();
            view.setCourseId(course.getId());
            view.setCourseCode(course.getCourseCode());
            view.setCourseName(course.getCourseName());
            view.setSemester(course.getSemester());
            view.setAssignmentCount(assignmentCount);
            view.setSubmissionCount(submissionCount);
            view.setReviewedCount(reviewedCount);
            view.setAverageScore(avgScore);
            view.setHighRiskAssignments(highRiskAssignments);
            courseStats.add(view);
        }

        List<StudentStatsView> studentStats = new ArrayList<>();
        for (Map.Entry<Long, List<Submission>> entry : submissionsByStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<Submission> studentSubmissions = entry.getValue();
            int reviewedCount = 0;
            BigDecimal scoreSum = BigDecimal.ZERO;
            LocalDateTime latestSubmitTime = null;
            for (Submission submission : studentSubmissions) {
                if (latestSubmitTime == null || submission.getSubmitTime().isAfter(latestSubmitTime)) {
                    latestSubmitTime = submission.getSubmitTime();
                }
                SubmissionReview review = reviewMap.get(submission.getId());
                if (review != null && review.getScore() != null) {
                    reviewedCount++;
                    scoreSum = scoreSum.add(review.getScore());
                }
            }
            BigDecimal avgScore = reviewedCount > 0
                ? scoreSum.divide(BigDecimal.valueOf(reviewedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            StudentStatsView view = new StudentStatsView();
            view.setStudentId(studentId);
            view.setSubmissionCount(studentSubmissions.size());
            view.setReviewedCount(reviewedCount);
            view.setAverageScore(avgScore);
            view.setLatestSubmitTime(latestSubmitTime);
            studentStats.add(view);
        }

        overview.setCourseStats(courseStats);
        overview.setAssignmentStats(assignmentStats);
        overview.setStudentStats(studentStats);
        return overview;
    }

    public List<SubmissionTrendPointView> submissionTrend(SysUser actor, int days) {
        int safeDays = Math.min(Math.max(1, days), 90);
        List<Course> courses = courseService.listAll(actor);
        if (courses.isEmpty()) {
            return fillEmptyDays(safeDays);
        }

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<Assignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
            .in(Assignment::getCourseId, courseIds));

        if (assignments.isEmpty()) {
            return fillEmptyDays(safeDays);
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        LocalDateTime startTime = LocalDate.now().minusDays(safeDays - 1).atStartOfDay();
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
            .in(Submission::getAssignmentId, assignmentIds)
            .ge(Submission::getSubmitTime, startTime));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            String dateKey = submission.getSubmitTime().toLocalDate().format(formatter);
            countMap.merge(dateKey, 1L, Long::sum);
        }

        List<SubmissionTrendPointView> result = new ArrayList<>();
        for (int i = safeDays - 1; i >= 0; i--) {
            String dateKey = LocalDate.now().minusDays(i).format(formatter);
            SubmissionTrendPointView point = new SubmissionTrendPointView();
            point.setDate(dateKey);
            point.setCount(countMap.getOrDefault(dateKey, 0L));
            result.add(point);
        }
        return result;
    }

    private List<SubmissionTrendPointView> fillEmptyDays(int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<SubmissionTrendPointView> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            SubmissionTrendPointView point = new SubmissionTrendPointView();
            point.setDate(LocalDate.now().minusDays(i).format(formatter));
            point.setCount(0L);
            result.add(point);
        }
        return result;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

