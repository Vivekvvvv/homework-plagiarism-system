package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.dto.PlagiarismEvalCaseCreateRequest;
import com.example.homework.domain.entity.PlagiarismEvalCase;
import com.example.homework.domain.entity.PlagiarismEvalRun;
import com.example.homework.domain.vo.PlagiarismEvalReportView;
import com.example.homework.mapper.PlagiarismEvalCaseMapper;
import com.example.homework.mapper.PlagiarismEvalRunMapper;
import com.example.homework.util.SimHashUtil;
import com.example.homework.util.TextSimilarityUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlagiarismEvalService {

    private final PlagiarismEvalCaseMapper plagiarismEvalCaseMapper;
    private final PlagiarismEvalRunMapper plagiarismEvalRunMapper;

    public PlagiarismEvalService(PlagiarismEvalCaseMapper plagiarismEvalCaseMapper,
                                 PlagiarismEvalRunMapper plagiarismEvalRunMapper) {
        this.plagiarismEvalCaseMapper = plagiarismEvalCaseMapper;
        this.plagiarismEvalRunMapper = plagiarismEvalRunMapper;
    }

    public PlagiarismEvalCase createCase(PlagiarismEvalCaseCreateRequest request) {
        PlagiarismEvalCase row = new PlagiarismEvalCase();
        row.setCaseName(shortText(request.getCaseName(), 120));
        row.setTextA(request.getTextA().trim());
        row.setTextB(request.getTextB().trim());
        row.setExpectedRiskLevel(request.getExpectedRiskLevel());
        row.setPredictedRiskLevel(null);
        row.setNote(shortText(request.getNote(), 500));
        row.setEnabled(1);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        plagiarismEvalCaseMapper.insert(row);
        return row;
    }

    public List<PlagiarismEvalCase> listCases(Integer enabled) {
        LambdaQueryWrapper<PlagiarismEvalCase> query = new LambdaQueryWrapper<>();
        if (enabled != null) {
            query.eq(PlagiarismEvalCase::getEnabled, enabled);
        }
        query.orderByDesc(PlagiarismEvalCase::getId);
        return plagiarismEvalCaseMapper.selectList(query);
    }

    public List<PlagiarismEvalCase> runEvaluation(BigDecimal thresholdInput,
                                                  BigDecimal simhashWeightInput,
                                                  BigDecimal jaccardWeightInput) {
        BigDecimal threshold = resolveThreshold(thresholdInput);
        BigDecimal simhashWeight = resolveSimhashWeight(simhashWeightInput);
        BigDecimal jaccardWeight = resolveJaccardWeight(jaccardWeightInput);

        List<PlagiarismEvalCase> rows = listCases(1);
        for (PlagiarismEvalCase row : rows) {
            long hashA = SimHashUtil.simHash64(row.getTextA());
            long hashB = SimHashUtil.simHash64(row.getTextB());
            int hamming = SimHashUtil.hammingDistance(hashA, hashB);

            BigDecimal simhashSimilarity = BigDecimal.valueOf(SimHashUtil.similarityByHamming(hamming))
                .setScale(4, RoundingMode.HALF_UP);
            BigDecimal jaccardSimilarity = TextSimilarityUtil.jaccardSimilarity(row.getTextA(), row.getTextB());
            BigDecimal fused = TextSimilarityUtil.fusedSimilarity(simhashSimilarity, jaccardSimilarity, simhashWeight, jaccardWeight);

            row.setSimhashSimilarity(simhashSimilarity);
            row.setJaccardSimilarity(jaccardSimilarity);
            row.setFusedSimilarity(fused);
            row.setPredictedRiskLevel(riskLevel(fused, threshold));
            row.setEvaluatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            plagiarismEvalCaseMapper.updateById(row);
        }
        return rows;
    }

    public PlagiarismEvalReportView report() {
        List<PlagiarismEvalCase> rows = listCases(1);
        List<PlagiarismEvalCase> evaluated = rows.stream()
            .filter(row -> row.getPredictedRiskLevel() != null && row.getExpectedRiskLevel() != null)
            .toList();

        int total = rows.size();
        int evaluatedCount = evaluated.size();
        int[][] matrix = new int[4][4];
        int correct = 0;
        for (PlagiarismEvalCase row : evaluated) {
            int expected = safeRisk(row.getExpectedRiskLevel());
            int predicted = safeRisk(row.getPredictedRiskLevel());
            matrix[expected][predicted]++;
            if (expected == predicted) {
                correct++;
            }
        }

        BigDecimal accuracy = ratio(correct, evaluatedCount);

        List<PlagiarismEvalReportView.PerRiskMetric> perRiskMetrics = new ArrayList<>();
        BigDecimal precisionSum = BigDecimal.ZERO;
        BigDecimal recallSum = BigDecimal.ZERO;
        BigDecimal f1Sum = BigDecimal.ZERO;
        for (int risk = 1; risk <= 3; risk++) {
            int tp = matrix[risk][risk];
            int fp = matrix[1][risk] + matrix[2][risk] + matrix[3][risk] - tp;
            int fn = matrix[risk][1] + matrix[risk][2] + matrix[risk][3] - tp;

            BigDecimal precision = ratio(tp, tp + fp);
            BigDecimal recall = ratio(tp, tp + fn);
            BigDecimal f1 = f1(precision, recall);

            PlagiarismEvalReportView.PerRiskMetric metric = new PlagiarismEvalReportView.PerRiskMetric();
            metric.setRiskLevel(risk);
            metric.setPrecision(precision);
            metric.setRecall(recall);
            metric.setF1(f1);
            perRiskMetrics.add(metric);

            precisionSum = precisionSum.add(precision);
            recallSum = recallSum.add(recall);
            f1Sum = f1Sum.add(f1);
        }

        List<PlagiarismEvalReportView.ConfusionRow> confusionRows = new ArrayList<>();
        for (int expected = 1; expected <= 3; expected++) {
            PlagiarismEvalReportView.ConfusionRow row = new PlagiarismEvalReportView.ConfusionRow();
            row.setExpectedRiskLevel(expected);
            row.setPredictedLow(matrix[expected][1]);
            row.setPredictedMedium(matrix[expected][2]);
            row.setPredictedHigh(matrix[expected][3]);
            confusionRows.add(row);
        }

        PlagiarismEvalReportView report = new PlagiarismEvalReportView();
        report.setTotalCases(total);
        report.setEvaluatedCases(evaluatedCount);
        report.setAccuracy(accuracy);
        report.setMacroPrecision(precisionSum.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP));
        report.setMacroRecall(recallSum.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP));
        report.setMacroF1(f1Sum.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP));
        report.setPerRiskMetrics(perRiskMetrics);
        report.setConfusionMatrix(confusionRows);
        return report;
    }

    public PlagiarismEvalRun recordRun(BigDecimal thresholdInput,
                                       BigDecimal simhashWeightInput,
                                       BigDecimal jaccardWeightInput,
                                       Long actorId) {
        PlagiarismEvalReportView report = report();
        PlagiarismEvalRun run = new PlagiarismEvalRun();
        run.setThreshold(resolveThreshold(thresholdInput));
        run.setSimhashWeight(resolveSimhashWeight(simhashWeightInput));
        run.setJaccardWeight(resolveJaccardWeight(jaccardWeightInput));
        run.setTotalCases(report.getTotalCases());
        run.setEvaluatedCases(report.getEvaluatedCases());
        run.setAccuracy(report.getAccuracy());
        run.setMacroPrecision(report.getMacroPrecision());
        run.setMacroRecall(report.getMacroRecall());
        run.setMacroF1(report.getMacroF1());
        run.setRunBy(actorId);
        run.setCreatedAt(LocalDateTime.now());
        plagiarismEvalRunMapper.insert(run);
        return run;
    }

    public List<PlagiarismEvalRun> listRuns(Integer limit) {
        int safeLimit = limit == null ? 20 : Math.min(Math.max(1, limit), 200);
        return plagiarismEvalRunMapper.selectList(new LambdaQueryWrapper<PlagiarismEvalRun>()
            .orderByDesc(PlagiarismEvalRun::getId)
            .last("LIMIT " + safeLimit));
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

    private int safeRisk(Integer risk) {
        if (risk == null || risk < 1 || risk > 3) {
            return 1;
        }
        return risk;
    }

    private BigDecimal ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal f1(BigDecimal precision, BigDecimal recall) {
        BigDecimal sum = precision.add(recall);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return precision.multiply(recall).multiply(BigDecimal.valueOf(2))
            .divide(sum, 4, RoundingMode.HALF_UP);
    }

    private String shortText(String input, int maxLen) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String cleaned = input.replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }

    private BigDecimal resolveThreshold(BigDecimal thresholdInput) {
        return thresholdInput == null ? BigDecimal.valueOf(0.70) : thresholdInput.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveSimhashWeight(BigDecimal simhashWeightInput) {
        return simhashWeightInput == null ? BigDecimal.valueOf(0.70) : simhashWeightInput.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveJaccardWeight(BigDecimal jaccardWeightInput) {
        return jaccardWeightInput == null ? BigDecimal.valueOf(0.30) : jaccardWeightInput.setScale(4, RoundingMode.HALF_UP);
    }
}
