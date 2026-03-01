package com.example.homework.service;

import com.example.homework.domain.entity.PlagiarismPairResult;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.vo.PlagiarismReportPairView;
import com.example.homework.domain.vo.PlagiarismTaskReportView;
import com.example.homework.util.CsvReportSupport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlagiarismReportBuilder {

    private PlagiarismReportBuilder() {
    }

    public static PlagiarismTaskReportView buildTaskReport(PlagiarismTask task, List<PlagiarismPairResult> pairs) {
        long lowCount = pairs.stream().filter(p -> p.getRiskLevel() != null && p.getRiskLevel() == 1).count();
        long mediumCount = pairs.stream().filter(p -> p.getRiskLevel() != null && p.getRiskLevel() == 2).count();
        long highCount = pairs.stream().filter(p -> p.getRiskLevel() != null && p.getRiskLevel() == 3).count();

        List<PlagiarismReportPairView> topPairs = new ArrayList<>();
        int topLimit = Math.min(20, pairs.size());
        for (int i = 0; i < topLimit; i++) {
            PlagiarismPairResult pair = pairs.get(i);
            PlagiarismReportPairView item = new PlagiarismReportPairView();
            item.setId(pair.getId());
            item.setSubmissionAId(pair.getSubmissionAId());
            item.setSubmissionBId(pair.getSubmissionBId());
            item.setSimilarity(pair.getSimilarity());
            item.setRiskLevel(pair.getRiskLevel());
            item.setHammingDistance(pair.getHammingDistance());
            topPairs.add(item);
        }

        PlagiarismTaskReportView report = new PlagiarismTaskReportView();
        report.setAssignmentId(task.getAssignmentId());
        report.setTaskId(task.getId());
        report.setStatus(task.getStatus());
        report.setAlgorithm(task.getAlgorithm());
        report.setThreshold(task.getThreshold());
        report.setSimhashWeight(task.getSimhashWeight());
        report.setJaccardWeight(task.getJaccardWeight());
        report.setTotalPairs(task.getTotalPairs());
        report.setHighRiskPairs(task.getHighRiskPairs());
        report.setRetryCount(task.getRetryCount());
        report.setMaxRetry(task.getMaxRetry());
        report.setRunTimeoutSeconds(task.getRunTimeoutSeconds());
        report.setCreatedAt(task.getCreatedAt());
        report.setStartedAt(task.getStartedAt());
        report.setFinishedAt(task.getFinishedAt());
        report.setLowRiskCount(lowCount);
        report.setMediumRiskCount(mediumCount);
        report.setHighRiskCount(highCount);
        report.setTopPairs(topPairs);
        return report;
    }

    public static byte[] buildPairsCsv(List<PlagiarismPairResult> pairs) {
        StringBuilder sb = new StringBuilder();
        CsvReportSupport.appendMeta(sb, "plagiarism_pairs", Map.of(
            "rows", String.valueOf(pairs.size())
        ));
        sb.append("pairId,submissionAId,submissionBId,fusedSimilarity,simhashSimilarity,jaccardSimilarity,hammingDistance,riskLevel,matchedFragments,explainJson").append("\n");
        for (PlagiarismPairResult pair : pairs) {
            sb.append(pair.getId()).append(",");
            sb.append(pair.getSubmissionAId()).append(",");
            sb.append(pair.getSubmissionBId()).append(",");
            sb.append(pair.getSimilarity()).append(",");
            sb.append(pair.getSimhashSimilarity()).append(",");
            sb.append(pair.getJaccardSimilarity()).append(",");
            sb.append(pair.getHammingDistance()).append(",");
            sb.append(pair.getRiskLevel()).append(",");
            sb.append(CsvReportSupport.csvEscape(pair.getMatchedFragmentsJson())).append(",");
            sb.append(CsvReportSupport.csvEscape(pair.getExplainJson())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] buildAssignmentReportCsv(PlagiarismTaskReportView report, List<PlagiarismPairResult> pairs) {
        StringBuilder sb = new StringBuilder();
        CsvReportSupport.appendMeta(sb, "plagiarism_assignment_report", Map.of(
            "assignmentId", String.valueOf(report.getAssignmentId()),
            "taskId", String.valueOf(report.getTaskId())
        ));
        sb.append("Section,Field,Value").append("\n");
        sb.append("Summary,assignmentId,").append(report.getAssignmentId()).append("\n");
        sb.append("Summary,taskId,").append(report.getTaskId()).append("\n");
        sb.append("Summary,status,").append(report.getStatus()).append("\n");
        sb.append("Summary,algorithm,").append(report.getAlgorithm()).append("\n");
        sb.append("Summary,threshold,").append(report.getThreshold()).append("\n");
        sb.append("Summary,simhashWeight,").append(report.getSimhashWeight()).append("\n");
        sb.append("Summary,jaccardWeight,").append(report.getJaccardWeight()).append("\n");
        sb.append("Summary,totalPairs,").append(report.getTotalPairs()).append("\n");
        sb.append("Summary,highRiskPairs,").append(report.getHighRiskPairs()).append("\n");
        sb.append("Summary,retryCount,").append(report.getRetryCount()).append("\n");
        sb.append("Summary,maxRetry,").append(report.getMaxRetry()).append("\n");
        sb.append("Summary,runTimeoutSeconds,").append(report.getRunTimeoutSeconds()).append("\n");
        sb.append("Summary,createdAt,").append(report.getCreatedAt()).append("\n");
        sb.append("Summary,startedAt,").append(report.getStartedAt()).append("\n");
        sb.append("Summary,finishedAt,").append(report.getFinishedAt()).append("\n");

        sb.append("\n");
        sb.append("Section,Level,Count").append("\n");
        sb.append("RiskDistribution,LOW,").append(report.getLowRiskCount()).append("\n");
        sb.append("RiskDistribution,MEDIUM,").append(report.getMediumRiskCount()).append("\n");
        sb.append("RiskDistribution,HIGH,").append(report.getHighRiskCount()).append("\n");

        sb.append("\n");
        sb.append("TopRiskyPairs,pairId,submissionAId,submissionBId,similarity,riskLevel,hammingDistance").append("\n");
        int topLimit = Math.min(20, pairs.size());
        for (int i = 0; i < topLimit; i++) {
            PlagiarismPairResult pair = pairs.get(i);
            sb.append("TopRiskyPairs,")
                .append(pair.getId()).append(",")
                .append(pair.getSubmissionAId()).append(",")
                .append(pair.getSubmissionBId()).append(",")
                .append(pair.getSimilarity()).append(",")
                .append(pair.getRiskLevel()).append(",")
                .append(pair.getHammingDistance()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
