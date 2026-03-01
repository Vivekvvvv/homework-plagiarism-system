package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.PageResult;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.dto.PlagiarismTaskCreateRequest;
import com.example.homework.domain.entity.PlagiarismPairResult;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.PlagiarismTaskLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.PlagiarismTaskReportView;
import com.example.homework.domain.vo.PlagiarismTaskTrendPointView;
import com.example.homework.mapper.PlagiarismPairResultMapper;
import com.example.homework.mapper.PlagiarismTaskLogMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.security.UserRole;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PlagiarismService {

    private final PlagiarismTaskMapper plagiarismTaskMapper;
    private final PlagiarismPairResultMapper plagiarismPairResultMapper;
    private final PlagiarismTaskLogMapper plagiarismTaskLogMapper;
    private final PlagiarismTaskRunnerService plagiarismTaskRunnerService;
    private final AssignmentService assignmentService;
    private final AuthzService authzService;
    private final AuditLogService auditLogService;

    public PlagiarismService(PlagiarismTaskMapper plagiarismTaskMapper,
                             PlagiarismPairResultMapper plagiarismPairResultMapper,
                             PlagiarismTaskLogMapper plagiarismTaskLogMapper,
                             PlagiarismTaskRunnerService plagiarismTaskRunnerService,
                             AssignmentService assignmentService,
                             AuthzService authzService,
                             AuditLogService auditLogService) {
        this.plagiarismTaskMapper = plagiarismTaskMapper;
        this.plagiarismPairResultMapper = plagiarismPairResultMapper;
        this.plagiarismTaskLogMapper = plagiarismTaskLogMapper;
        this.plagiarismTaskRunnerService = plagiarismTaskRunnerService;
        this.assignmentService = assignmentService;
        this.authzService = authzService;
        this.auditLogService = auditLogService;
    }

    public PlagiarismTask createTask(PlagiarismTaskCreateRequest request, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        assignmentService.getByIdWithAccess(request.getAssignmentId(), actor);
        PlagiarismTask task = createTaskInternal(
            request.getAssignmentId(),
            request.getThreshold(),
            request.getSimhashWeight(),
            request.getJaccardWeight(),
            request.getIdempotencyKey(),
            request.getMaxRetry(),
            request.getRunTimeoutSeconds(),
            actor.getId()
        );
        auditLogService.log(actor, AuditAction.PLAGIARISM_TASK_CREATE.name(), "plagiarism_task",
            String.valueOf(task.getId()),
            "assignmentId=" + request.getAssignmentId() + ",threshold=" + request.getThreshold(),
            "/api/v1/plagiarism/tasks", "POST");
        return task;
    }

    public PlagiarismTask retryTask(Long taskId, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        PlagiarismTask task = getTaskByIdWithAccess(taskId, actor);
        if (task.getStatus() != null && (task.getStatus() == 0 || task.getStatus() == 1)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Task is still pending or running");
        }
        PlagiarismTask retried = createTaskInternal(
            task.getAssignmentId(),
            task.getThreshold(),
            task.getSimhashWeight(),
            task.getJaccardWeight(),
            null,
            task.getMaxRetry(),
            task.getRunTimeoutSeconds(),
            actor.getId()
        );
        auditLogService.log(actor, AuditAction.PLAGIARISM_TASK_RETRY.name(), "plagiarism_task",
            String.valueOf(taskId), "newTaskId=" + retried.getId(),
            "/api/v1/plagiarism/tasks/" + taskId + "/retry", "POST");
        return retried;
    }

    public PlagiarismTask cancelTask(Long taskId, SysUser actor) {
        authzService.requireRoleIn(actor, UserRole.ADMIN, UserRole.TEACHER);
        PlagiarismTask task = getTaskByIdWithAccess(taskId, actor);
        Integer status = task.getStatus();
        if (status != null && (status == 2 || status == 3 || status == 4)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Task cannot be canceled in current status");
        }
        task.setStatus(4);
        task.setFinishedAt(LocalDateTime.now());
        task.setErrorMessage("Canceled by user");
        plagiarismTaskMapper.updateById(task);
        logTask(taskId, "CANCELED", "Task canceled by user");
        auditLogService.log(actor, AuditAction.PLAGIARISM_TASK_CANCEL.name(), "plagiarism_task",
            String.valueOf(taskId), null, "/api/v1/plagiarism/tasks/" + taskId + "/cancel", "PATCH");
        return task;
    }

    public byte[] exportPairsCsv(Long taskId, Integer riskLevel, BigDecimal minSimilarity, SysUser actor) {
        getTaskByIdWithAccess(taskId, actor);
        List<PlagiarismPairResult> pairs = plagiarismPairResultMapper.selectList(
            buildPairQuery(taskId, riskLevel, minSimilarity)
                .orderByDesc(PlagiarismPairResult::getSimilarity)
                .orderByAsc(PlagiarismPairResult::getId)
        );
        return PlagiarismReportBuilder.buildPairsCsv(pairs);
    }

    public byte[] exportAssignmentReportCsv(Long assignmentId, SysUser actor) {
        PlagiarismTask task = latestTaskByAssignment(assignmentId, actor);
        List<PlagiarismPairResult> pairs = fetchPairsByTask(task.getId());
        PlagiarismTaskReportView report = PlagiarismReportBuilder.buildTaskReport(task, pairs);
        return PlagiarismReportBuilder.buildAssignmentReportCsv(report, pairs);
    }

    public PlagiarismTaskReportView getTaskReport(Long taskId, SysUser actor) {
        PlagiarismTask task = getTaskByIdWithAccess(taskId, actor);
        return PlagiarismReportBuilder.buildTaskReport(task, fetchPairsByTask(task.getId()));
    }

    private PlagiarismTask createTaskInternal(Long assignmentId,
                                              BigDecimal thresholdInput,
                                              BigDecimal simhashWeightInput,
                                              BigDecimal jaccardWeightInput,
                                              String idempotencyKeyInput,
                                              Integer maxRetryInput,
                                              Integer runTimeoutSecondsInput,
                                              Long createdBy) {
        BigDecimal threshold = thresholdInput == null
            ? BigDecimal.valueOf(0.70)
            : thresholdInput.setScale(4, RoundingMode.HALF_UP);
        BigDecimal[] normalizedWeights = normalizeWeights(simhashWeightInput, jaccardWeightInput);

        String idempotencyKey = shortKey(idempotencyKeyInput);
        if (StringUtils.hasText(idempotencyKey)) {
            PlagiarismTask existing = plagiarismTaskMapper.selectOne(new LambdaQueryWrapper<PlagiarismTask>()
                .eq(PlagiarismTask::getAssignmentId, assignmentId)
                .eq(PlagiarismTask::getIdempotencyKey, idempotencyKey)
                .in(PlagiarismTask::getStatus, List.of(0, 1, 2))
                .orderByDesc(PlagiarismTask::getId)
                .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }

        int maxRetry = maxRetryInput == null ? 1 : Math.min(Math.max(0, maxRetryInput), 5);
        int runTimeoutSeconds = runTimeoutSecondsInput == null ? 120 : Math.min(Math.max(30, runTimeoutSecondsInput), 900);

        PlagiarismTask task = new PlagiarismTask();
        task.setAssignmentId(assignmentId);
        task.setAlgorithm("SIMHASH+JACCARD");
        task.setStatus(0);
        task.setThreshold(threshold);
        task.setSimhashWeight(normalizedWeights[0]);
        task.setJaccardWeight(normalizedWeights[1]);
        task.setTotalPairs(0);
        task.setHighRiskPairs(0);
        task.setIdempotencyKey(idempotencyKey);
        task.setRetryCount(0);
        task.setMaxRetry(maxRetry);
        task.setRunTimeoutSeconds(runTimeoutSeconds);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());
        plagiarismTaskMapper.insert(task);

        logTask(task.getId(), "CREATED", "Task created, waiting for async execution");
        plagiarismTaskRunnerService.runTaskAsync(task.getId());
        return task;
    }

    public List<PlagiarismTask> listTasksByAssignment(Long assignmentId, SysUser actor) {
        assignmentService.getByIdWithAccess(assignmentId, actor);
        return plagiarismTaskMapper.selectList(new LambdaQueryWrapper<PlagiarismTask>()
            .eq(PlagiarismTask::getAssignmentId, assignmentId)
            .orderByDesc(PlagiarismTask::getId));
    }

    public PlagiarismTask latestTaskByAssignment(Long assignmentId, SysUser actor) {
        assignmentService.getByIdWithAccess(assignmentId, actor);
        PlagiarismTask task = plagiarismTaskMapper.selectOne(new LambdaQueryWrapper<PlagiarismTask>()
            .eq(PlagiarismTask::getAssignmentId, assignmentId)
            .orderByDesc(PlagiarismTask::getId)
            .last("LIMIT 1"));
        if (task == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "No plagiarism task found for this assignment");
        }
        return task;
    }

    public PlagiarismTask getTaskById(Long taskId) {
        PlagiarismTask task = plagiarismTaskMapper.selectOne(new LambdaQueryWrapper<PlagiarismTask>()
            .eq(PlagiarismTask::getId, taskId)
            .last("LIMIT 1"));
        if (task == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Plagiarism task not found");
        }
        return task;
    }

    private PlagiarismTask getTaskByIdWithAccess(Long taskId, SysUser actor) {
        PlagiarismTask task = getTaskById(taskId);
        assignmentService.getByIdWithAccess(task.getAssignmentId(), actor);
        return task;
    }

    public PageResult<PlagiarismPairResult> listPairsByTask(Long taskId,
                                                            Integer riskLevel,
                                                            BigDecimal minSimilarity,
                                                            long pageNo,
                                                            long pageSize,
                                                            SysUser actor) {
        getTaskByIdWithAccess(taskId, actor);
        long safePageNo = Math.max(1, pageNo);
        long safePageSize = Math.min(Math.max(1, pageSize), 100);

        LambdaQueryWrapper<PlagiarismPairResult> countWrapper = buildPairQuery(taskId, riskLevel, minSimilarity);
        long total = plagiarismPairResultMapper.selectCount(countWrapper);
        if (total == 0) {
            return new PageResult<>(0, safePageNo, safePageSize, List.of());
        }

        long offset = (safePageNo - 1) * safePageSize;
        LambdaQueryWrapper<PlagiarismPairResult> pageWrapper = buildPairQuery(taskId, riskLevel, minSimilarity)
            .orderByDesc(PlagiarismPairResult::getSimilarity)
            .orderByAsc(PlagiarismPairResult::getId)
            .last("LIMIT " + offset + "," + safePageSize);
        List<PlagiarismPairResult> records = plagiarismPairResultMapper.selectList(pageWrapper);

        return new PageResult<>(
            total,
            safePageNo,
            safePageSize,
            records
        );
    }

    public PlagiarismPairResult getPairById(Long pairId, SysUser actor) {
        PlagiarismPairResult pair = plagiarismPairResultMapper.selectOne(new LambdaQueryWrapper<PlagiarismPairResult>()
            .eq(PlagiarismPairResult::getId, pairId)
            .last("LIMIT 1"));
        if (pair == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Pair result not found");
        }
        getTaskByIdWithAccess(pair.getTaskId(), actor);
        return pair;
    }

    public List<PlagiarismTaskLog> listTaskLogs(Long taskId, SysUser actor) {
        getTaskByIdWithAccess(taskId, actor);
        return plagiarismTaskLogMapper.selectList(new LambdaQueryWrapper<PlagiarismTaskLog>()
            .eq(PlagiarismTaskLog::getTaskId, taskId)
            .orderByDesc(PlagiarismTaskLog::getId)
            .last("LIMIT 200"));
    }

    public List<PlagiarismTaskTrendPointView> listAssignmentTrend(Long assignmentId, SysUser actor) {
        return listAssignmentTrend(assignmentId, null, null, 20, actor);
    }

    public List<PlagiarismTaskTrendPointView> listAssignmentTrend(Long assignmentId,
                                                                  LocalDateTime startAt,
                                                                  LocalDateTime endAt,
                                                                  Integer limitInput,
                                                                  SysUser actor) {
        assignmentService.getByIdWithAccess(assignmentId, actor);
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "startAt cannot be later than endAt");
        }

        int safeLimit = limitInput == null ? 20 : Math.min(Math.max(1, limitInput), 200);

        LambdaQueryWrapper<PlagiarismTask> query = new LambdaQueryWrapper<PlagiarismTask>()
            .eq(PlagiarismTask::getAssignmentId, assignmentId);
        if (startAt != null) {
            query.ge(PlagiarismTask::getCreatedAt, startAt);
        }
        if (endAt != null) {
            query.le(PlagiarismTask::getCreatedAt, endAt);
        }
        query.orderByDesc(PlagiarismTask::getId).last("LIMIT " + safeLimit);

        List<PlagiarismTask> tasks = plagiarismTaskMapper.selectList(query);

        if (tasks.isEmpty()) {
            return List.of();
        }

        List<PlagiarismTaskTrendPointView> result = new ArrayList<>();
        for (PlagiarismTask task : tasks) {
            int totalPairs = task.getTotalPairs() == null ? 0 : task.getTotalPairs();
            int highRiskPairs = task.getHighRiskPairs() == null ? 0 : task.getHighRiskPairs();

            BigDecimal highRiskRate = BigDecimal.ZERO;
            if (totalPairs > 0) {
                highRiskRate = BigDecimal.valueOf(highRiskPairs)
                    .divide(BigDecimal.valueOf(totalPairs), 4, RoundingMode.HALF_UP);
            }

            PlagiarismTaskTrendPointView point = new PlagiarismTaskTrendPointView();
            point.setTaskId(task.getId());
            point.setStatus(task.getStatus());
            point.setThreshold(task.getThreshold());
            point.setTotalPairs(totalPairs);
            point.setHighRiskPairs(highRiskPairs);
            point.setHighRiskRate(highRiskRate);
            point.setCreatedAt(task.getCreatedAt());
            point.setFinishedAt(task.getFinishedAt());
            result.add(point);
        }

        Collections.reverse(result);
        return result;
    }

    public void logTask(Long taskId, String phase, String message) {
        PlagiarismTaskLog log = new PlagiarismTaskLog();
        log.setTaskId(taskId);
        log.setPhase(phase);
        log.setMessage(shortMessage(message));
        log.setCreatedAt(LocalDateTime.now());
        plagiarismTaskLogMapper.insert(log);
    }

    private List<PlagiarismPairResult> fetchPairsByTask(Long taskId) {
        return plagiarismPairResultMapper.selectList(
            new LambdaQueryWrapper<PlagiarismPairResult>()
                .eq(PlagiarismPairResult::getTaskId, taskId)
                .orderByDesc(PlagiarismPairResult::getSimilarity)
                .orderByAsc(PlagiarismPairResult::getId)
        );
    }

    private LambdaQueryWrapper<PlagiarismPairResult> buildPairQuery(Long taskId,
                                                                    Integer riskLevel,
                                                                    BigDecimal minSimilarity) {
        LambdaQueryWrapper<PlagiarismPairResult> wrapper = new LambdaQueryWrapper<PlagiarismPairResult>()
            .eq(PlagiarismPairResult::getTaskId, taskId);

        if (riskLevel != null) {
            wrapper.eq(PlagiarismPairResult::getRiskLevel, riskLevel);
        }

        if (minSimilarity != null) {
            wrapper.ge(PlagiarismPairResult::getSimilarity, minSimilarity.setScale(4, RoundingMode.HALF_UP));
        }
        return wrapper;
    }

    private String shortMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String cleaned = message.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }

    private String shortKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String cleaned = key.replaceAll("\\s+", "").trim();
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    private BigDecimal[] normalizeWeights(BigDecimal simhashWeightInput, BigDecimal jaccardWeightInput) {
        BigDecimal simhashWeight = simhashWeightInput == null
            ? BigDecimal.valueOf(0.70)
            : simhashWeightInput.max(BigDecimal.ZERO);
        BigDecimal jaccardWeight = jaccardWeightInput == null
            ? BigDecimal.valueOf(0.30)
            : jaccardWeightInput.max(BigDecimal.ZERO);
        BigDecimal sum = simhashWeight.add(jaccardWeight);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[]{BigDecimal.valueOf(0.70), BigDecimal.valueOf(0.30)};
        }
        return new BigDecimal[]{
            simhashWeight.divide(sum, 4, RoundingMode.HALF_UP),
            jaccardWeight.divide(sum, 4, RoundingMode.HALF_UP)
        };
    }
}
