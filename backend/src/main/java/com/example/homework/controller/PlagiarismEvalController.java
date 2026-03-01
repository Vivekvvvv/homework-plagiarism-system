package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.domain.dto.PlagiarismEvalCaseCreateRequest;
import com.example.homework.domain.entity.PlagiarismEvalRun;
import com.example.homework.domain.entity.PlagiarismEvalCase;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.service.AuditLogService;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.PlagiarismEvalService;
import com.example.homework.domain.vo.PlagiarismEvalReportView;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import static com.example.homework.controller.ControllerSupport.requireTeacherOrAdmin;

@RestController
@RequestMapping("/api/v1/plagiarism/evaluation")
public class PlagiarismEvalController {

    private final PlagiarismEvalService plagiarismEvalService;
    private final AuthService authService;
    private final AuthzService authzService;
    private final AuditLogService auditLogService;

    public PlagiarismEvalController(PlagiarismEvalService plagiarismEvalService,
                                    AuthService authService,
                                    AuthzService authzService,
                                    AuditLogService auditLogService) {
        this.plagiarismEvalService = plagiarismEvalService;
        this.authService = authService;
        this.authzService = authzService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/cases")
    public ApiResponse<PlagiarismEvalCase> createCase(Authentication authentication,
                                                      @Valid @RequestBody PlagiarismEvalCaseCreateRequest request) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        PlagiarismEvalCase created = plagiarismEvalService.createCase(request);
        auditLogService.log(actor, AuditAction.EVAL_CASE_CREATE.name(), "plagiarism_eval_case",
            String.valueOf(created.getId()), request.getCaseName(), "/api/v1/plagiarism/evaluation/cases", "POST");
        return ApiResponse.ok(created);
    }

    @GetMapping("/cases")
    public ApiResponse<List<PlagiarismEvalCase>> listCases(Authentication authentication,
                                                           @RequestParam(required = false) Integer enabled) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismEvalService.listCases(enabled));
    }

    @PostMapping("/run")
    public ApiResponse<List<PlagiarismEvalCase>> run(Authentication authentication,
                                                     @RequestParam(required = false) BigDecimal threshold,
                                                     @RequestParam(required = false) BigDecimal simhashWeight,
                                                     @RequestParam(required = false) BigDecimal jaccardWeight) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        List<PlagiarismEvalCase> rows = plagiarismEvalService.runEvaluation(threshold, simhashWeight, jaccardWeight);
        plagiarismEvalService.recordRun(threshold, simhashWeight, jaccardWeight, actor.getId());
        auditLogService.log(actor, AuditAction.EVAL_RUN.name(), "plagiarism_eval_case", null,
            "threshold=" + threshold + ",simhashWeight=" + simhashWeight + ",jaccardWeight=" + jaccardWeight,
            "/api/v1/plagiarism/evaluation/run", "POST");
        return ApiResponse.ok(rows);
    }

    @GetMapping("/report")
    public ApiResponse<PlagiarismEvalReportView> report(Authentication authentication) {
        SysUser actor = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismEvalService.report());
    }

    @GetMapping("/runs")
    public ApiResponse<List<PlagiarismEvalRun>> runs(Authentication authentication,
                                                     @RequestParam(defaultValue = "20") Integer limit) {
        requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismEvalService.listRuns(limit));
    }
}
