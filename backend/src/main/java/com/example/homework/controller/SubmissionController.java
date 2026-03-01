package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.dto.SubmissionCreateRequest;
import com.example.homework.domain.entity.Submission;
import com.example.homework.domain.entity.SubmissionText;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.SubmissionEvolutionPointView;
import com.example.homework.domain.vo.SubmissionView;
import com.example.homework.service.AuthService;
import com.example.homework.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.homework.controller.ControllerSupport.currentUser;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final AuthService authService;

    public SubmissionController(SubmissionService submissionService, AuthService authService) {
        this.submissionService = submissionService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<Submission> create(@Valid @RequestBody SubmissionCreateRequest request,
                                          Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(submissionService.create(request, actor));
    }

    @GetMapping
    public ApiResponse<List<SubmissionView>> listByAssignment(@RequestParam Long assignmentId,
                                                              Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(submissionService.listByAssignmentId(assignmentId, actor));
    }

    @GetMapping("/evolution")
    public ApiResponse<List<SubmissionEvolutionPointView>> evolution(@RequestParam Long assignmentId,
                                                                     @RequestParam Long studentId,
                                                                     Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(submissionService.listEvolution(assignmentId, studentId, actor));
    }

    @GetMapping("/{submissionId}/text")
    public ApiResponse<SubmissionText> text(@PathVariable Long submissionId,
                                            Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(submissionService.getTextBySubmissionId(submissionId, actor));
    }
}
