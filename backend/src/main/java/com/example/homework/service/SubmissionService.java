package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.SubmissionCreateRequest;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.FileStorage;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionText;
import com.example.homework.domain.entity.SubmissionReview;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.SubmissionEvolutionPointView;
import com.example.homework.domain.vo.SubmissionView;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import com.example.homework.mapper.SubmissionTextMapper;
import com.example.homework.util.HashUtil;
import com.example.homework.util.TextSimilarityUtil;
import com.example.homework.security.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final SubmissionTextMapper submissionTextMapper;
    private final SubmissionReviewMapper submissionReviewMapper;
    private final AssignmentMapper assignmentMapper;
    private final FileStorageService fileStorageService;
    private final TextExtractService textExtractService;
    private final CourseService courseService;
    private final AuthService authService;
    private final AuthzService authzService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public SubmissionService(SubmissionMapper submissionMapper,
                             SubmissionTextMapper submissionTextMapper,
                             SubmissionReviewMapper submissionReviewMapper,
                             AssignmentMapper assignmentMapper,
                             FileStorageService fileStorageService,
                             TextExtractService textExtractService,
                             CourseService courseService,
                             AuthService authService,
                             AuthzService authzService,
                             AuditLogService auditLogService,
                             NotificationService notificationService) {
        this.submissionMapper = submissionMapper;
        this.submissionTextMapper = submissionTextMapper;
        this.submissionReviewMapper = submissionReviewMapper;
        this.assignmentMapper = assignmentMapper;
        this.fileStorageService = fileStorageService;
        this.textExtractService = textExtractService;
        this.courseService = courseService;
        this.authService = authService;
        this.authzService = authzService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Submission create(SubmissionCreateRequest request, SysUser actor) {
        Assignment assignment = validateAssignment(request.getAssignmentId());
        authzService.requireStudentSelfOrPrivileged(actor, request.getStudentId());

        String plainText;
        Long fileId = request.getFileId();
        int sourceType;

        if (fileId != null) {
            FileStorage fileStorage = fileStorageService.getById(fileId);
            byte[] bytes = fileStorageService.readFileBytes(fileStorage);
            plainText = textExtractService.extractTextFromBytes(fileStorage.getFileName(), fileStorage.getMimeType(), bytes);
            sourceType = 1;
        } else if (StringUtils.hasText(request.getRawText())) {
            plainText = request.getRawText().trim();
            sourceType = 2;
        } else {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "fileId and rawText cannot both be empty");
        }

        int nextVersion = getNextVersion(request.getAssignmentId(), request.getStudentId());
        String normalizedText = normalizeText(plainText);

        Submission submission = new Submission();
        submission.setAssignmentId(request.getAssignmentId());
        submission.setStudentId(request.getStudentId());
        submission.setSubmitTime(LocalDateTime.now());
        submission.setFileId(fileId);
        submission.setContentHash(HashUtil.sha256(normalizedText));
        submission.setVersionNo(nextVersion);
        submission.setSourceType(sourceType);
        submissionMapper.insert(submission);

        SubmissionText submissionText = new SubmissionText();
        submissionText.setSubmissionId(submission.getId());
        submissionText.setPlainText(plainText);
        submissionText.setTokenCount(countTokens(normalizedText));
        submissionText.setPreprocessVersion("v1");
        submissionText.setCreatedAt(LocalDateTime.now());
        submissionTextMapper.insert(submissionText);

        auditLogService.log(actor, AuditAction.SUBMISSION_CREATE.name(), "submission",
            String.valueOf(submission.getId()),
            "assignmentId=" + assignment.getId() + ",studentId=" + request.getStudentId(),
            "/api/v1/submissions", "POST");

        Course course = courseService.getById(assignment.getCourseId());
        notificationService.createNotification(
            course.getTeacherId(),
            "新增作业提交",
            "学生#" + request.getStudentId() + " 提交了作业《" + assignment.getTitle() + "》v" + nextVersion,
            "info",
            "submission",
            String.valueOf(submission.getId())
        );

        return submission;
    }

    public List<SubmissionView> listByAssignmentId(Long assignmentId, SysUser actor) {
        ensureSubmissionAccess(assignmentId, actor);
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
            .eq(Submission::getAssignmentId, assignmentId)
            .orderByDesc(Submission::getId));

        if (authService.isStudent(actor)) {
            submissions = submissions.stream()
                .filter(item -> item.getStudentId().equals(actor.getId()))
                .toList();
        }

        if (submissions.isEmpty()) {
            return List.of();
        }

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<SubmissionText> texts = submissionTextMapper.selectList(new LambdaQueryWrapper<SubmissionText>()
            .in(SubmissionText::getSubmissionId, submissionIds));

        Map<Long, SubmissionText> textMap = new HashMap<>();
        for (SubmissionText text : texts) {
            textMap.put(text.getSubmissionId(), text);
        }

        return submissions.stream().map(item -> {
            SubmissionView view = new SubmissionView();
            view.setId(item.getId());
            view.setAssignmentId(item.getAssignmentId());
            view.setStudentId(item.getStudentId());
            view.setSubmitTime(item.getSubmitTime());
            view.setVersionNo(item.getVersionNo());
            view.setSourceType(item.getSourceType());
            view.setContentHash(item.getContentHash());
            SubmissionText text = textMap.get(item.getId());
            view.setTokenCount(text == null ? 0 : text.getTokenCount());
            return view;
        }).toList();
    }

    public List<SubmissionEvolutionPointView> listEvolution(Long assignmentId, Long studentId, SysUser actor) {
        ensureSubmissionAccess(assignmentId, actor);
        if (authService.isStudent(actor) && !actor.getId().equals(studentId)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Students can only view their own submission evolution");
        }

        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
            .eq(Submission::getAssignmentId, assignmentId)
            .eq(Submission::getStudentId, studentId)
            .orderByAsc(Submission::getVersionNo)
            .orderByAsc(Submission::getId));

        if (submissions.isEmpty()) {
            return List.of();
        }

        List<Long> ids = submissions.stream().map(Submission::getId).toList();
        List<SubmissionText> texts = submissionTextMapper.selectList(new LambdaQueryWrapper<SubmissionText>()
            .in(SubmissionText::getSubmissionId, ids));
        Map<Long, SubmissionText> textMap = new HashMap<>();
        for (SubmissionText text : texts) {
            textMap.put(text.getSubmissionId(), text);
        }

        List<SubmissionReview> reviews = submissionReviewMapper.selectList(new LambdaQueryWrapper<SubmissionReview>()
            .in(SubmissionReview::getSubmissionId, ids));
        Map<Long, SubmissionReview> reviewMap = new HashMap<>();
        for (SubmissionReview review : reviews) {
            reviewMap.put(review.getSubmissionId(), review);
        }

        List<SubmissionEvolutionPointView> points = new java.util.ArrayList<>();
        Submission previous = null;
        for (Submission current : submissions) {
            SubmissionText currentText = textMap.get(current.getId());
            String currentPlain = currentText == null ? "" : currentText.getPlainText();

            SubmissionEvolutionPointView point = new SubmissionEvolutionPointView();
            point.setSubmissionId(current.getId());
            point.setAssignmentId(current.getAssignmentId());
            point.setStudentId(current.getStudentId());
            point.setVersionNo(current.getVersionNo());
            point.setTokenCount(currentText == null ? 0 : currentText.getTokenCount());
            point.setSubmitTime(current.getSubmitTime());
            SubmissionReview review = reviewMap.get(current.getId());
            if (review != null) {
                point.setScore(review.getScore());
                point.setReviewedAt(review.getReviewedAt());
            }

            if (previous == null) {
                point.setSimilarityToPrevious(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
                point.setChangeRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                point.setMinutesSincePrevious(0L);
            } else {
                SubmissionText previousText = textMap.get(previous.getId());
                String prevPlain = previousText == null ? "" : previousText.getPlainText();
                BigDecimal similarity = TextSimilarityUtil.jaccardSimilarity(prevPlain, currentPlain);
                BigDecimal changeRate = BigDecimal.ONE.subtract(similarity).setScale(4, RoundingMode.HALF_UP);
                long minutes = Duration.between(previous.getSubmitTime(), current.getSubmitTime()).toMinutes();

                point.setSimilarityToPrevious(similarity);
                point.setChangeRate(changeRate);
                point.setMinutesSincePrevious(Math.max(0, minutes));
            }

            points.add(point);
            previous = current;
        }

        return points;
    }

    public SubmissionText getTextBySubmissionId(Long submissionId, SysUser actor) {
        Submission submission = requireSubmissionAccessible(submissionId, actor);
        authzService.requireStudentSelfOrPrivileged(actor, submission.getStudentId());

        SubmissionText text = submissionTextMapper.selectOne(new LambdaQueryWrapper<SubmissionText>()
            .eq(SubmissionText::getSubmissionId, submissionId)
            .last("LIMIT 1"));
        if (text == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Submission text not found");
        }
        return text;
    }

    public Submission requireSubmissionAccessible(Long submissionId, SysUser actor) {
        Submission submission = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
            .eq(Submission::getId, submissionId)
            .last("LIMIT 1"));
        if (submission == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Submission not found");
        }
        ensureSubmissionAccess(submission.getAssignmentId(), actor);
        return submission;
    }

    private Assignment validateAssignment(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectOne(new LambdaQueryWrapper<Assignment>()
            .eq(Assignment::getId, assignmentId)
            .last("LIMIT 1"));
        if (assignment == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Assignment not found");
        }
        return assignment;
    }

    private void ensureSubmissionAccess(Long assignmentId, SysUser actor) {
        Assignment assignment = validateAssignment(assignmentId);
        if (authService.isAdmin(actor)) {
            return;
        }
        if (authService.isTeacher(actor)) {
            Course course = courseService.getById(assignment.getCourseId());
            if (!actor.getId().equals(course.getTeacherId())) {
                throw new BusinessException(ErrorCodes.FORBIDDEN, "Teacher can only access submissions of own course");
            }
            return;
        }
        authzService.requireRoleIn(actor, UserRole.STUDENT);
    }

    private int getNextVersion(Long assignmentId, Long studentId) {
        Submission latest = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
            .eq(Submission::getAssignmentId, assignmentId)
            .eq(Submission::getStudentId, studentId)
            .orderByDesc(Submission::getVersionNo)
            .last("LIMIT 1"));
        if (latest == null) {
            return 1;
        }
        return latest.getVersionNo() + 1;
    }

    private String normalizeText(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private int countTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return text.split(" ").length;
    }
}
