package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.common.PageResult;
import com.example.homework.domain.dto.PlagiarismTaskCreateRequest;
import com.example.homework.domain.entity.PlagiarismPairResult;
import com.example.homework.domain.entity.PlagiarismTask;
import com.example.homework.domain.entity.PlagiarismTaskLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.PlagiarismTaskReportView;
import com.example.homework.domain.vo.PlagiarismTaskTrendPointView;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.PlagiarismService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.homework.controller.ControllerSupport.requireTeacherOrAdmin;

@RestController
@RequestMapping("/api/v1/plagiarism")
public class PlagiarismController {

    private final PlagiarismService plagiarismService;
    private final AuthService authService;
    private final AuthzService authzService;

    public PlagiarismController(PlagiarismService plagiarismService, AuthService authService, AuthzService authzService) {
        this.plagiarismService = plagiarismService;
        this.authService = authService;
        this.authzService = authzService;
    }

    @PostMapping("/tasks")
    public ApiResponse<PlagiarismTask> createTask(@Valid @RequestBody PlagiarismTaskCreateRequest request,
                                                  Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.createTask(request, user));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<PlagiarismTask>> listTasks(@RequestParam Long assignmentId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.listTasksByAssignment(assignmentId, user));
    }

    @GetMapping("/tasks/latest")
    public ApiResponse<PlagiarismTask> latestTask(@RequestParam Long assignmentId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.latestTaskByAssignment(assignmentId, user));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<PlagiarismTask> getTask(@PathVariable Long taskId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.getTaskById(taskId));
    }

    @GetMapping("/tasks/{taskId}/report")
    public ApiResponse<PlagiarismTaskReportView> taskReport(@PathVariable Long taskId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.getTaskReport(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/logs")
    public ApiResponse<List<PlagiarismTaskLog>> listTaskLogs(@PathVariable Long taskId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.listTaskLogs(taskId, user));
    }

    @PatchMapping("/tasks/{taskId}/cancel")
    public ApiResponse<PlagiarismTask> cancelTask(@PathVariable Long taskId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.cancelTask(taskId, user));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<PlagiarismTask> retryTask(@PathVariable Long taskId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.retryTask(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/pairs")
    public ApiResponse<PageResult<PlagiarismPairResult>> listPairs(@PathVariable Long taskId,
                                                                   @RequestParam(required = false) Integer riskLevel,
                                                                   @RequestParam(required = false) BigDecimal minSimilarity,
                                                                   @RequestParam(defaultValue = "1") Long pageNo,
                                                                   @RequestParam(defaultValue = "20") Long pageSize,
                                                                   Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.listPairsByTask(taskId, riskLevel, minSimilarity, pageNo, pageSize, user));
    }

    @GetMapping("/tasks/{taskId}/pairs/export")
    public ResponseEntity<ByteArrayResource> exportPairs(@PathVariable Long taskId,
                                                         @RequestParam(required = false) Integer riskLevel,
                                                         @RequestParam(required = false) BigDecimal minSimilarity,
                                                         Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        byte[] csv = plagiarismService.exportPairsCsv(taskId, riskLevel, minSimilarity, user);

        ByteArrayResource resource = new ByteArrayResource(csv);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("plagiarism_task_" + taskId + ".csv").build());
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentLength(csv.length);
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/assignments/{assignmentId}/report/export")
    public ResponseEntity<ByteArrayResource> exportAssignmentReport(@PathVariable Long assignmentId,
                                                                    Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        byte[] csv = plagiarismService.exportAssignmentReportCsv(assignmentId, user);

        ByteArrayResource resource = new ByteArrayResource(csv);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("assignment_" + assignmentId + "_plagiarism_report.csv").build()
        );
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentLength(csv.length);
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/assignments/{assignmentId}/trend")
    public ApiResponse<List<PlagiarismTaskTrendPointView>> assignmentTrend(
        @PathVariable Long assignmentId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @RequestParam(defaultValue = "20") Integer limit,
        Authentication authentication
    ) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.listAssignmentTrend(assignmentId, startAt, endAt, limit, user));
    }

    @GetMapping("/pairs/{pairId}")
    public ApiResponse<PlagiarismPairResult> pairDetail(@PathVariable Long pairId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(plagiarismService.getPairById(pairId, user));
    }
}
