package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.AssignmentReviewRubricUpsertRequest;
import com.example.homework.domain.dto.BatchReviewUpsertRequest;
import com.example.homework.domain.dto.ReviewDimensionScoreRequest;
import com.example.homework.domain.dto.ReviewRubricItemRequest;
import com.example.homework.domain.dto.SubmissionReviewUpsertRequest;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.AssignmentReviewRubric;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionReview;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.AssignmentReviewRubricView;
import com.example.homework.domain.vo.ReviewSuggestionView;
import com.example.homework.domain.vo.SubmissionReviewSummaryView;
import com.example.homework.domain.vo.SubmissionReviewView;
import com.example.homework.domain.vo.SubmissionView;
import com.example.homework.mapper.AssignmentReviewRubricMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import com.example.homework.security.UserRole;
import com.example.homework.util.CsvReportSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private final SubmissionReviewMapper submissionReviewMapper;
    private final SubmissionService submissionService;
    private final AssignmentService assignmentService;
    private final AssignmentReviewRubricMapper assignmentReviewRubricMapper;
    private final ObjectMapper objectMapper;
    private final AuthzService authzService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ReviewService(SubmissionReviewMapper submissionReviewMapper,
                         SubmissionService submissionService,
                         AssignmentService assignmentService,
                         AssignmentReviewRubricMapper assignmentReviewRubricMapper,
                         ObjectMapper objectMapper,
                         AuthzService authzService,
                         AuditLogService auditLogService,
                         NotificationService notificationService) {
        this.submissionReviewMapper = submissionReviewMapper;
        this.submissionService = submissionService;
        this.assignmentService = assignmentService;
        this.assignmentReviewRubricMapper = assignmentReviewRubricMapper;
        this.objectMapper = objectMapper;
        this.authzService = authzService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<SubmissionReview> batchUpsertReview(BatchReviewUpsertRequest request, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        List<SubmissionReview> results = new ArrayList<>();
        for (SubmissionReviewUpsertRequest item : request.getReviews()) {
            results.add(upsertReview(item, actor));
        }
        return results;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionReview upsertReview(SubmissionReviewUpsertRequest request, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        Submission submission = submissionService.requireSubmissionAccessible(request.getSubmissionId(), actor);

        String dimensionScoresJson = null;
        BigDecimal score;
        List<ReviewDimensionScoreRequest> dimensionScores = request.getDimensionScores() == null
            ? List.of()
            : request.getDimensionScores();

        if (!dimensionScores.isEmpty()) {
            score = scoreByDimension(submission.getAssignmentId(), dimensionScores);
            dimensionScoresJson = writeJson(dimensionScores);
        } else {
            score = request.getScore();
        }

        if (score == null) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "score is required when dimensionScores is empty");
        }

        score = score.setScale(2, RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Score must be between 0 and 100");
        }

        ReviewSuggestionView suggestion = buildSuggestion(score);
        String autoComment = suggestion.getSuggestion();

        SubmissionReview review = submissionReviewMapper.selectOne(new LambdaQueryWrapper<SubmissionReview>()
            .eq(SubmissionReview::getSubmissionId, request.getSubmissionId())
            .last("LIMIT 1"));

        if (review == null) {
            review = new SubmissionReview();
            review.setSubmissionId(request.getSubmissionId());
            review.setAssignmentId(submission.getAssignmentId());
            review.setCreatedAt(LocalDateTime.now());
        }

        review.setReviewerId(actor.getId());
        review.setScore(score);
        review.setAutoComment(autoComment);
        review.setDimensionScoresJson(dimensionScoresJson);
        review.setComment(trimComment(request.getComment(), autoComment));
        review.setReviewedAt(LocalDateTime.now());

        if (review.getId() == null) {
            submissionReviewMapper.insert(review);
        } else {
            submissionReviewMapper.updateById(review);
        }

        auditLogService.log(actor, AuditAction.REVIEW_UPSERT.name(), "submission_review",
            String.valueOf(review.getId()),
            "submissionId=" + review.getSubmissionId() + ",score=" + review.getScore(),
            "/api/v1/reviews", "POST");

        Assignment assignment = assignmentService.getById(submission.getAssignmentId());
        String content = "作业《" + assignment.getTitle() + "》v" + submission.getVersionNo() + " 已完成评阅，得分 " + score;
        notificationService.createNotification(
            submission.getStudentId(),
            "评阅结果已更新",
            content,
            "success",
            "review",
            String.valueOf(review.getId())
        );
        return review;
    }

    public AssignmentReviewRubricView upsertRubric(AssignmentReviewRubricUpsertRequest request, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        assignmentService.getByIdWithAccess(request.getAssignmentId(), actor);

        if (request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "rubric items cannot be empty");
        }

        Map<String, BigDecimal> uniqueWeights = new LinkedHashMap<>();
        List<AssignmentReviewRubricView.RubricItemView> normalizedItems = new ArrayList<>();
        for (ReviewRubricItemRequest item : request.getItems()) {
            if (!StringUtils.hasText(item.getDimension())) {
                continue;
            }
            String dimension = item.getDimension().trim();
            BigDecimal weight = item.getWeight() == null ? BigDecimal.ZERO : item.getWeight();
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            uniqueWeights.put(dimension, weight);

            AssignmentReviewRubricView.RubricItemView view = new AssignmentReviewRubricView.RubricItemView();
            view.setDimension(dimension);
            view.setWeight(weight.setScale(2, RoundingMode.HALF_UP));
            view.setDescription(trimText(item.getDescription(), 300));
            normalizedItems.add(view);
        }

        if (uniqueWeights.isEmpty()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "rubric items must contain valid positive weights");
        }

        String rubricJson = writeJson(normalizedItems);
        AssignmentReviewRubric record = assignmentReviewRubricMapper.selectOne(new LambdaQueryWrapper<AssignmentReviewRubric>()
            .eq(AssignmentReviewRubric::getAssignmentId, request.getAssignmentId())
            .last("LIMIT 1"));

        if (record == null) {
            record = new AssignmentReviewRubric();
            record.setAssignmentId(request.getAssignmentId());
            record.setRubricJson(rubricJson);
            record.setCreatedAt(LocalDateTime.now());
            assignmentReviewRubricMapper.insert(record);
        } else {
            record.setRubricJson(rubricJson);
            assignmentReviewRubricMapper.updateById(record);
        }

        AssignmentReviewRubricView view = getRubricInternal(request.getAssignmentId());
        auditLogService.log(actor, AuditAction.REVIEW_RUBRIC_UPSERT.name(), "assignment_review_rubric",
            String.valueOf(request.getAssignmentId()),
            "items=" + normalizedItems.size(),
            "/api/v1/reviews/rubric", "PUT");
        return view;
    }

    public AssignmentReviewRubricView getRubric(Long assignmentId, SysUser actor) {
        assignmentService.getByIdWithAccess(assignmentId, actor);
        return getRubricInternal(assignmentId);
    }

    public ReviewSuggestionView buildSuggestion(BigDecimal score) {
        BigDecimal safeScore = score == null ? BigDecimal.ZERO : score.setScale(2, RoundingMode.HALF_UP);
        ReviewSuggestionView view = new ReviewSuggestionView();
        view.setScore(safeScore);

        if (safeScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            view.setLevel("优秀");
            view.setSuggestion("结构完整、论证充分、表达规范，建议补充方法边界与实验局限性的讨论。");
            return view;
        }
        if (safeScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            view.setLevel("良好");
            view.setSuggestion("整体完成度较高，关键点覆盖较好，建议补充数据对比与结论支撑细节。");
            return view;
        }
        if (safeScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            view.setLevel("及格");
            view.setSuggestion("基础内容完整，但深度与细节不足，建议加强实验分析与问题复盘。");
            return view;
        }

        view.setLevel("需改进");
        view.setSuggestion("核心要求覆盖不足，建议先补齐关键功能并给出可验证结果，再完善文档表达。");
        return view;
    }

    public List<SubmissionReviewView> listByAssignmentId(Long assignmentId, SysUser actor) {
        List<SubmissionView> submissions = submissionService.listByAssignmentId(assignmentId, actor);
        if (submissions.isEmpty()) {
            return List.of();
        }

        List<Long> submissionIds = submissions.stream().map(SubmissionView::getId).toList();
        List<SubmissionReview> reviews = submissionReviewMapper.selectList(new LambdaQueryWrapper<SubmissionReview>()
            .in(SubmissionReview::getSubmissionId, submissionIds));

        Map<Long, SubmissionReview> reviewMap = new HashMap<>();
        for (SubmissionReview review : reviews) {
            reviewMap.put(review.getSubmissionId(), review);
        }

        return submissions.stream().map(item -> {
            SubmissionReviewView view = new SubmissionReviewView();
            view.setSubmissionId(item.getId());
            view.setAssignmentId(item.getAssignmentId());
            view.setStudentId(item.getStudentId());
            view.setVersionNo(item.getVersionNo());
            view.setSourceType(item.getSourceType());
            view.setTokenCount(item.getTokenCount());
            view.setSubmitTime(item.getSubmitTime());

            SubmissionReview review = reviewMap.get(item.getId());
            if (review != null) {
                view.setReviewId(review.getId());
                view.setReviewerId(review.getReviewerId());
                view.setScore(review.getScore());
                view.setComment(review.getComment());
                view.setAutoComment(review.getAutoComment());
                view.setDimensionScoresJson(review.getDimensionScoresJson());
                view.setReviewedAt(review.getReviewedAt());
            }
            return view;
        }).toList();
    }

    public SubmissionReview getBySubmissionId(Long submissionId, SysUser actor) {
        Submission submission = submissionService.requireSubmissionAccessible(submissionId, actor);
        authzService.requireStudentSelfOrPrivileged(actor, submission.getStudentId());

        SubmissionReview review = submissionReviewMapper.selectOne(new LambdaQueryWrapper<SubmissionReview>()
            .eq(SubmissionReview::getSubmissionId, submissionId)
            .last("LIMIT 1"));
        if (review == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "评阅记录不存在");
        }
        return review;
    }

    public SubmissionReviewSummaryView summaryByAssignmentId(Long assignmentId, SysUser actor) {
        List<SubmissionReviewView> rows = listByAssignmentId(assignmentId, actor);
        int total = rows.size();
        int reviewed = (int) rows.stream().filter(item -> item.getScore() != null).count();

        BigDecimal reviewedRate = BigDecimal.ZERO;
        if (total > 0) {
            reviewedRate = BigDecimal.valueOf(reviewed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        BigDecimal averageScore = BigDecimal.ZERO;
        if (reviewed > 0) {
            BigDecimal sum = rows.stream()
                .map(SubmissionReviewView::getScore)
                .filter(score -> score != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageScore = sum.divide(BigDecimal.valueOf(reviewed), 2, RoundingMode.HALF_UP);
        }

        int passCount = (int) rows.stream()
            .map(SubmissionReviewView::getScore)
            .filter(score -> score != null && score.compareTo(BigDecimal.valueOf(60)) >= 0)
            .count();

        BigDecimal passRate = BigDecimal.ZERO;
        if (reviewed > 0) {
            passRate = BigDecimal.valueOf(passCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(reviewed), 2, RoundingMode.HALF_UP);
        }

        SubmissionReviewSummaryView summary = new SubmissionReviewSummaryView();
        summary.setTotalSubmissions(total);
        summary.setReviewedSubmissions(reviewed);
        summary.setReviewedRate(reviewedRate);
        summary.setAverageScore(averageScore);
        summary.setPassCount(passCount);
        summary.setPassRate(passRate);
        return summary;
    }

    public byte[] exportReviewsCsv(Long assignmentId, SysUser actor) {
        List<SubmissionReviewView> rows = listByAssignmentId(assignmentId, actor);
        StringBuilder sb = new StringBuilder();
        CsvReportSupport.appendMeta(sb, "review_export", Map.of(
            "assignmentId", String.valueOf(assignmentId),
            "rows", String.valueOf(rows.size())
        ));
        sb.append("submissionId,assignmentId,studentId,versionNo,sourceType,tokenCount,submitTime,score,reviewedAt,reviewerId,comment,autoComment,dimensionScoresJson").append("\n");
        for (SubmissionReviewView row : rows) {
            sb.append(row.getSubmissionId()).append(",");
            sb.append(row.getAssignmentId()).append(",");
            sb.append(row.getStudentId()).append(",");
            sb.append(row.getVersionNo()).append(",");
            sb.append(row.getSourceType()).append(",");
            sb.append(row.getTokenCount()).append(",");
            sb.append(row.getSubmitTime()).append(",");
            sb.append(row.getScore() == null ? "" : row.getScore()).append(",");
            sb.append(row.getReviewedAt() == null ? "" : row.getReviewedAt()).append(",");
            sb.append(row.getReviewerId() == null ? "" : row.getReviewerId()).append(",");
            sb.append(CsvReportSupport.csvEscape(row.getComment())).append(",");
            sb.append(CsvReportSupport.csvEscape(row.getAutoComment())).append(",");
            sb.append(CsvReportSupport.csvEscape(row.getDimensionScoresJson())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BigDecimal scoreByDimension(Long assignmentId, List<ReviewDimensionScoreRequest> dimensions) {
        Map<String, BigDecimal> weightMap = rubricWeightMap(assignmentId);

        BigDecimal weightedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal averageSum = BigDecimal.ZERO;

        for (ReviewDimensionScoreRequest dimension : dimensions) {
            if (!StringUtils.hasText(dimension.getDimension()) || dimension.getScore() == null) {
                continue;
            }

            BigDecimal score = dimension.getScore().max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
            averageSum = averageSum.add(score);

            BigDecimal weight = weightMap.get(dimension.getDimension().trim());
            if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
                weightedScore = weightedScore.add(score.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            return weightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        if (dimensions.isEmpty()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "dimensionScores cannot be empty");
        }

        return averageSum.divide(BigDecimal.valueOf(dimensions.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> rubricWeightMap(Long assignmentId) {
        AssignmentReviewRubricView rubric = getRubricInternal(assignmentId);
        Map<String, BigDecimal> map = new HashMap<>();
        for (AssignmentReviewRubricView.RubricItemView item : rubric.getItems()) {
            if (StringUtils.hasText(item.getDimension()) && item.getWeight() != null && item.getWeight().compareTo(BigDecimal.ZERO) > 0) {
                map.put(item.getDimension().trim(), item.getWeight());
            }
        }
        return map;
    }

    private AssignmentReviewRubricView getRubricInternal(Long assignmentId) {
        AssignmentReviewRubric record = assignmentReviewRubricMapper.selectOne(new LambdaQueryWrapper<AssignmentReviewRubric>()
            .eq(AssignmentReviewRubric::getAssignmentId, assignmentId)
            .last("LIMIT 1"));

        AssignmentReviewRubricView view = new AssignmentReviewRubricView();
        view.setAssignmentId(assignmentId);

        if (record == null || !StringUtils.hasText(record.getRubricJson())) {
            view.setItems(defaultRubricItems());
            view.setUpdatedAt(null);
            return view;
        }

        view.setItems(readRubricItems(record.getRubricJson()));
        view.setUpdatedAt(record.getUpdatedAt());
        return view;
    }

    private List<AssignmentReviewRubricView.RubricItemView> defaultRubricItems() {
        List<AssignmentReviewRubricView.RubricItemView> items = new ArrayList<>();

        AssignmentReviewRubricView.RubricItemView item1 = new AssignmentReviewRubricView.RubricItemView();
        item1.setDimension("需求完成度");
        item1.setWeight(BigDecimal.valueOf(40));
        item1.setDescription("功能是否完整满足作业要求");
        items.add(item1);

        AssignmentReviewRubricView.RubricItemView item2 = new AssignmentReviewRubricView.RubricItemView();
        item2.setDimension("工程质量");
        item2.setWeight(BigDecimal.valueOf(35));
        item2.setDescription("代码结构、可维护性、异常处理");
        items.add(item2);

        AssignmentReviewRubricView.RubricItemView item3 = new AssignmentReviewRubricView.RubricItemView();
        item3.setDimension("文档与表达");
        item3.setWeight(BigDecimal.valueOf(25));
        item3.setDescription("文档完整性与说明清晰度");
        items.add(item3);

        return items;
    }

    private List<AssignmentReviewRubricView.RubricItemView> readRubricItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AssignmentReviewRubricView.RubricItemView>>() {
            });
        } catch (JsonProcessingException e) {
            return defaultRubricItems();
        }
    }

    private String trimComment(String input, String fallback) {
        if (!StringUtils.hasText(input)) {
            return fallback;
        }
        String cleaned = input.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 1000 ? cleaned.substring(0, 1000) : cleaned;
    }

    private String trimText(String input, int limit) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String cleaned = input.replaceAll("\\s+", " ").trim();
        return cleaned.length() > limit ? cleaned.substring(0, limit) : cleaned;
    }

    private String writeJson(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "failed to serialize json");
        }
    }
}
