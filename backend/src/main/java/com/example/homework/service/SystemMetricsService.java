package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionReview;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.SystemMetricsView;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.mapper.CourseMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import com.example.homework.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemMetricsService {

    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionReviewMapper submissionReviewMapper;
    private final PlagiarismTaskMapper plagiarismTaskMapper;

    public SystemMetricsService(SysUserMapper sysUserMapper,
                                CourseMapper courseMapper,
                                AssignmentMapper assignmentMapper,
                                SubmissionMapper submissionMapper,
                                SubmissionReviewMapper submissionReviewMapper,
                                PlagiarismTaskMapper plagiarismTaskMapper) {
        this.sysUserMapper = sysUserMapper;
        this.courseMapper = courseMapper;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.submissionReviewMapper = submissionReviewMapper;
        this.plagiarismTaskMapper = plagiarismTaskMapper;
    }

    public SystemMetricsView snapshot() {
        SystemMetricsView view = new SystemMetricsView();
        view.setUserCount(sysUserMapper.selectCount(null).intValue());
        view.setTeacherCount(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getRole, "TEACHER")).intValue());
        view.setStudentCount(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getRole, "STUDENT")).intValue());

        view.setCourseCount(courseMapper.selectCount(null).intValue());
        view.setAssignmentCount(assignmentMapper.selectCount(null).intValue());
        view.setSubmissionCount(submissionMapper.selectCount(null).intValue());
        view.setReviewCount(submissionReviewMapper.selectCount(null).intValue());
        view.setPlagiarismTaskCount(plagiarismTaskMapper.selectCount(null).intValue());

        List<PlagiarismTask> tasks = plagiarismTaskMapper.selectList(new LambdaQueryWrapper<PlagiarismTask>()
            .orderByDesc(PlagiarismTask::getId)
            .last("LIMIT 200"));
        int highRiskPairs = tasks.stream()
            .map(PlagiarismTask::getHighRiskPairs)
            .filter(value -> value != null)
            .mapToInt(Integer::intValue)
            .sum();
        view.setPlagiarismHighRiskPairs(highRiskPairs);

        Submission latestSubmission = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
            .orderByDesc(Submission::getSubmitTime)
            .last("LIMIT 1"));
        if (latestSubmission != null) {
            view.setLatestSubmissionTime(latestSubmission.getSubmitTime());
        }

        SubmissionReview latestReview = submissionReviewMapper.selectOne(new LambdaQueryWrapper<SubmissionReview>()
            .orderByDesc(SubmissionReview::getReviewedAt)
            .last("LIMIT 1"));
        if (latestReview != null) {
            view.setLatestReviewTime(latestReview.getReviewedAt());
        }

        PlagiarismTask latestTask = plagiarismTaskMapper.selectOne(new LambdaQueryWrapper<PlagiarismTask>()
            .orderByDesc(PlagiarismTask::getCreatedAt)
            .last("LIMIT 1"));
        if (latestTask != null) {
            view.setLatestPlagiarismTaskTime(latestTask.getCreatedAt());
        }

        return view;
    }
}

