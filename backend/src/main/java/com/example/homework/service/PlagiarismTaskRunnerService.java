package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.PlagiarismPairResult;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.PlagiarismTaskLog;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionText;
import com.example.homework.mapper.PlagiarismPairResultMapper;
import com.example.homework.mapper.PlagiarismTaskLogMapper;
import com.example.homework.mapper.PlagiarismTaskMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionTextMapper;
import com.example.homework.util.SimHashUtil;
import com.example.homework.util.TextSimilarityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlagiarismTaskRunnerService {

    private final PlagiarismTaskMapper plagiarismTaskMapper;
    private final PlagiarismPairResultMapper plagiarismPairResultMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionTextMapper submissionTextMapper;
    private final ObjectMapper objectMapper;
    private final PlagiarismTaskLogMapper plagiarismTaskLogMapper;
    private final NotificationService notificationService;

    public PlagiarismTaskRunnerService(PlagiarismTaskMapper plagiarismTaskMapper,
                                       PlagiarismPairResultMapper plagiarismPairResultMapper,
                                       SubmissionMapper submissionMapper,
                                       SubmissionTextMapper submissionTextMapper,
                                       ObjectMapper objectMapper,
                                       PlagiarismTaskLogMapper plagiarismTaskLogMapper,
                                       NotificationService notificationService) {
        this.plagiarismTaskMapper = plagiarismTaskMapper;
        this.plagiarismPairResultMapper = plagiarismPairResultMapper;
        this.submissionMapper = submissionMapper;
        this.submissionTextMapper = submissionTextMapper;
        this.objectMapper = objectMapper;
        this.plagiarismTaskLogMapper = plagiarismTaskLogMapper;
        this.notificationService = notificationService;
    }

    @Async("plagiarismTaskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void runTaskAsync(Long taskId) {
        PlagiarismTask task = plagiarismTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        if (task.getStatus() != null && task.getStatus() == 4) {
            return;
        }

        task.setStatus(1);
        task.setErrorMessage(null);
        task.setStartedAt(LocalDateTime.now());
        plagiarismTaskMapper.updateById(task);
        logTask(taskId, "STARTED", "任务已启动");

        try {
            int[] stats = execute(task);
            task.setTotalPairs(stats[0]);
            task.setHighRiskPairs(stats[1]);
            task.setStatus(2);
            task.setFinishedAt(LocalDateTime.now());
            task.setErrorMessage(null);
            plagiarismTaskMapper.updateById(task);
            logTask(taskId, "SUCCEEDED", "任务完成：总配对数=" + stats[0] + "，高风险配对数=" + stats[1]);
            String level = stats[1] > 0 ? "warning" : "success";
            notificationService.createNotification(
                task.getCreatedBy(),
                "查重任务完成",
                "作业#" + task.getAssignmentId() + " 查重完成，高风险对数 " + stats[1],
                level,
                "plagiarism_task",
                String.valueOf(task.getId())
            );
        } catch (TaskCanceledException ex) {
            task.setStatus(4);
            task.setFinishedAt(LocalDateTime.now());
            task.setErrorMessage("已被用户取消");
            plagiarismTaskMapper.updateById(task);
            logTask(taskId, "CANCELED", "执行期间任务被取消");
        } catch (Exception ex) {
            PlagiarismTask latest = plagiarismTaskMapper.selectById(taskId);
            if (latest != null && shouldRetry(latest)) {
                int nextRetry = (latest.getRetryCount() == null ? 0 : latest.getRetryCount()) + 1;
                latest.setRetryCount(nextRetry);
                latest.setStatus(0);
                latest.setErrorMessage(shortMessage(ex.getMessage()));
                latest.setFinishedAt(LocalDateTime.now());
                plagiarismTaskMapper.updateById(latest);

                logTask(taskId, "RETRY_SCHEDULED", "第 " + nextRetry + "/" + latest.getMaxRetry() + " 次重试：" + shortMessage(ex.getMessage()));
                runTaskAsync(taskId);
                return;
            }

            task.setStatus(3);
            task.setFinishedAt(LocalDateTime.now());
            task.setErrorMessage(shortMessage(ex.getMessage()));
            plagiarismTaskMapper.updateById(task);
            logTask(taskId, "FAILED", shortMessage(ex.getMessage()));
            notificationService.createNotification(
                task.getCreatedBy(),
                "查重任务失败",
                "作业#" + task.getAssignmentId() + " 查重失败：" + shortMessage(ex.getMessage()),
                "danger",
                "plagiarism_task",
                String.valueOf(task.getId())
            );
        }
    }

    private int[] execute(PlagiarismTask task) {
        List<Submission> allSubmissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
            .eq(Submission::getAssignmentId, task.getAssignmentId())
            .orderByAsc(Submission::getStudentId)
            .orderByDesc(Submission::getVersionNo)
            .orderByDesc(Submission::getId));

        Map<Long, Submission> latestByStudent = new LinkedHashMap<>();
        for (Submission submission : allSubmissions) {
            latestByStudent.putIfAbsent(submission.getStudentId(), submission);
        }
        List<Submission> submissions = latestByStudent.values().stream().toList();

        if (submissions.size() < 2) {
            logTask(task.getId(), "RUNNING", "有效提交不足2份，跳过配对匹配");
            return new int[]{0, 0};
        }
        logTask(task.getId(), "RUNNING", "开始对 " + submissions.size() + " 份提交进行配对匹配");

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<SubmissionText> textList = submissionTextMapper.selectList(new LambdaQueryWrapper<SubmissionText>()
            .in(SubmissionText::getSubmissionId, submissionIds));

        Map<Long, String> textMap = new HashMap<>();
        for (SubmissionText text : textList) {
            textMap.put(text.getSubmissionId(), text.getPlainText() == null ? "" : text.getPlainText());
        }

        Map<Long, Long> simHashMap = new HashMap<>();
        for (Submission submission : submissions) {
            String plainText = textMap.getOrDefault(submission.getId(), "");
            simHashMap.put(submission.getId(), SimHashUtil.simHash64(plainText));
        }

        int totalPairs = 0;
        int highRiskPairs = 0;
        BigDecimal threshold = task.getThreshold() == null ? BigDecimal.valueOf(0.70) : task.getThreshold();
        BigDecimal simhashWeight = task.getSimhashWeight() == null ? BigDecimal.valueOf(0.70) : task.getSimhashWeight();
        BigDecimal jaccardWeight = task.getJaccardWeight() == null ? BigDecimal.valueOf(0.30) : task.getJaccardWeight();
        int timeoutSeconds = task.getRunTimeoutSeconds() == null ? 120 : task.getRunTimeoutSeconds();
        long startedMillis = System.currentTimeMillis();

        for (int i = 0; i < submissions.size(); i++) {
            ensureTaskRunnable(task.getId(), startedMillis, timeoutSeconds);
            for (int j = i + 1; j < submissions.size(); j++) {
                ensureTaskRunnable(task.getId(), startedMillis, timeoutSeconds);
                Submission a = submissions.get(i);
                Submission b = submissions.get(j);

                String textA = textMap.getOrDefault(a.getId(), "");
                String textB = textMap.getOrDefault(b.getId(), "");

                int distance = SimHashUtil.hammingDistance(simHashMap.get(a.getId()), simHashMap.get(b.getId()));
                BigDecimal simhashSimilarity = BigDecimal.valueOf(SimHashUtil.similarityByHamming(distance)).setScale(4, RoundingMode.HALF_UP);
                BigDecimal jaccardSimilarity = TextSimilarityUtil.jaccardSimilarity(textA, textB);
                BigDecimal fusedSimilarity = TextSimilarityUtil.fusedSimilarity(simhashSimilarity, jaccardSimilarity, simhashWeight, jaccardWeight);

                int riskLevel = riskLevel(fusedSimilarity, threshold);
                if (riskLevel == 3) {
                    highRiskPairs++;
                }
                totalPairs++;

                PlagiarismPairResult pair = new PlagiarismPairResult();
                pair.setTaskId(task.getId());
                pair.setSubmissionAId(a.getId());
                pair.setSubmissionBId(b.getId());
                pair.setPairKey(pairKey(a.getId(), b.getId()));
                pair.setSimilarity(fusedSimilarity);
                pair.setSimhashSimilarity(simhashSimilarity);
                pair.setJaccardSimilarity(jaccardSimilarity);
                pair.setHammingDistance(distance);
                pair.setRiskLevel(riskLevel);
                pair.setMatchedFragmentsJson(buildMatchedFragmentsJson(textA, textB));
                pair.setExplainJson(buildExplainJson(simhashSimilarity, jaccardSimilarity, fusedSimilarity, threshold, riskLevel, textA, textB));
                pair.setCreatedAt(LocalDateTime.now());
                plagiarismPairResultMapper.insert(pair);
            }
        }

        return new int[]{totalPairs, highRiskPairs};
    }

    private int riskLevel(BigDecimal similarity, BigDecimal threshold) {
        BigDecimal highLine = threshold.add(BigDecimal.valueOf(0.15)).min(BigDecimal.ONE);
        if (similarity.compareTo(highLine) >= 0) {
            return 3;
        }
        if (similarity.compareTo(threshold) >= 0) {
            return 2;
        }
        return 1;
    }

    private String pairKey(Long a, Long b) {
        long left = Math.min(a, b);
        long right = Math.max(a, b);
        return left + "#" + right;
    }

    private String buildExplainJson(BigDecimal simhashSimilarity,
                                    BigDecimal jaccardSimilarity,
                                    BigDecimal fusedSimilarity,
                                    BigDecimal threshold,
                                    int riskLevel,
                                    String textA,
                                    String textB) {
        Map<String, Object> explain = new LinkedHashMap<>();
        explain.put("simhashSimilarity", simhashSimilarity);
        explain.put("jaccardSimilarity", jaccardSimilarity);
        explain.put("fusedSimilarity", fusedSimilarity);
        explain.put("threshold", threshold);
        explain.put("riskLevel", riskLevel);
        explain.put("riskReason", riskReason(riskLevel, fusedSimilarity, threshold));
        explain.put("overlapTokens", overlapTokens(textA, textB));

        try {
            return objectMapper.writeValueAsString(explain);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String riskReason(int riskLevel, BigDecimal fusedSimilarity, BigDecimal threshold) {
        if (riskLevel == 3) {
            return "融合相似度显著超出阈值";
        }
        if (riskLevel == 2) {
            return "融合相似度接近或超过阈值";
        }
        if (fusedSimilarity.compareTo(threshold.subtract(BigDecimal.valueOf(0.05)).max(BigDecimal.ZERO)) >= 0) {
            return "相似度低于阈值但接近";
        }
        return "相似度安全低于阈值";
    }

    private List<String> overlapTokens(String textA, String textB) {
        List<String> tokensA = TextSimilarityUtil.tokenize(textA);
        Set<String> tokenSetB = new LinkedHashSet<>(TextSimilarityUtil.tokenize(textB));
        Set<String> overlap = new LinkedHashSet<>();
        for (String token : tokensA) {
            if (tokenSetB.contains(token) && token.length() >= 3) {
                overlap.add(token);
            }
            if (overlap.size() >= 12) {
                break;
            }
        }
        return new ArrayList<>(overlap);
    }

    private String buildMatchedFragmentsJson(String textA, String textB) {
        String normalizedA = normalizeForSentenceMatch(textA);
        String normalizedB = normalizeForSentenceMatch(textB);
        if (!StringUtils.hasText(normalizedA) || !StringUtils.hasText(normalizedB)) {
            return "[]";
        }

        List<String> candidatesA = extractSentenceCandidates(textA);
        List<String> candidatesB = extractSentenceCandidates(textB);
        Set<String> matched = new LinkedHashSet<>();

        for (String item : candidatesA) {
            String normalizedItem = normalizeForSentenceMatch(item);
            if (normalizedItem.length() >= 8 && normalizedB.contains(normalizedItem)) {
                matched.add(item.trim());
            }
            if (matched.size() >= 12) {
                break;
            }
        }

        if (matched.size() < 12) {
            for (String item : candidatesB) {
                String normalizedItem = normalizeForSentenceMatch(item);
                if (normalizedItem.length() >= 8 && normalizedA.contains(normalizedItem)) {
                    matched.add(item.trim());
                }
                if (matched.size() >= 12) {
                    break;
                }
            }
        }

        if (matched.isEmpty()) {
            matched.addAll(fallbackTokenFragments(textA, textB));
        }

        try {
            return objectMapper.writeValueAsString(new ArrayList<>(matched));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> extractSentenceCandidates(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String[] parts = text.split("[\\u3002\\uFF01\\uFF1F\\uFF1B;.!?\\r\\n]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.trim().replaceAll("\\s+", " ");
            if (cleaned.length() >= 8) {
                if (cleaned.length() > 90) {
                    result.add(cleaned.substring(0, 90));
                } else {
                    result.add(cleaned);
                }
            }
        }
        return result;
    }

    private Set<String> fallbackTokenFragments(String textA, String textB) {
        List<String> tokensA = TextSimilarityUtil.tokenize(textA);
        Set<String> tokensB = new LinkedHashSet<>(TextSimilarityUtil.tokenize(textB));
        Set<String> matched = new LinkedHashSet<>();
        for (String token : tokensA) {
            if (token.length() >= 4 && tokensB.contains(token)) {
                matched.add(token);
            }
            if (matched.size() >= 12) {
                break;
            }
        }
        return matched;
    }

    private String normalizeForSentenceMatch(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.toLowerCase()
            .replaceAll("[\\p{Punct}\\r\\n\\t]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String shortMessage(String msg) {
        if (!StringUtils.hasText(msg)) {
            return "任务执行失败";
        }
        String cleaned = msg.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 480 ? cleaned.substring(0, 480) : cleaned;
    }

    private boolean shouldRetry(PlagiarismTask task) {
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetry = task.getMaxRetry() == null ? 0 : task.getMaxRetry();
        return retryCount < maxRetry;
    }

    private void ensureTaskRunnable(Long taskId, long startedMillis, int timeoutSeconds) {
        PlagiarismTask latest = plagiarismTaskMapper.selectById(taskId);
        if (latest == null) {
            throw new TaskCanceledException();
        }
        if (latest.getStatus() != null && latest.getStatus() == 4) {
            throw new TaskCanceledException();
        }
        long elapsed = System.currentTimeMillis() - startedMillis;
        if (elapsed > timeoutSeconds * 1000L) {
            throw new TaskTimeoutException();
        }
    }

    private void logTask(Long taskId, String phase, String message) {
        PlagiarismTaskLog log = new PlagiarismTaskLog();
        log.setTaskId(taskId);
        log.setPhase(phase);
        log.setMessage(shortMessage(message));
        log.setCreatedAt(LocalDateTime.now());
        plagiarismTaskLogMapper.insert(log);
    }

    private static class TaskCanceledException extends RuntimeException {
    }

    private static class TaskTimeoutException extends RuntimeException {
    }
}
