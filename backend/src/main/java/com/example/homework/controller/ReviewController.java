package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.dto.AssignmentReviewRubricUpsertRequest;
import com.example.homework.domain.dto.SubmissionReviewUpsertRequest;
import com.example.homework.domain.entity.SubmissionReview;
import com.example.homework.domain.vo.AssignmentReviewRubricView;
import com.example.homework.domain.vo.ReviewSuggestionView;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.domain.vo.SubmissionReviewSummaryView;
import com.example.homework.domain.vo.SubmissionReviewView;
import com.example.homework.service.AuthService;
import com.example.homework.service.AuthzService;
import com.example.homework.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.homework.controller.ControllerSupport.currentUser;
import static com.example.homework.controller.ControllerSupport.requireTeacherOrAdmin;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthService authService;
    private final AuthzService authzService;

    public ReviewController(ReviewService reviewService, AuthService authService, AuthzService authzService) {
        this.reviewService = reviewService;
        this.authService = authService;
        this.authzService = authzService;
    }

    @PostMapping
    public ApiResponse<SubmissionReview> upsert(@Valid @RequestBody SubmissionReviewUpsertRequest request,
                                                Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(reviewService.upsertReview(request, user));
    }

    @GetMapping
    public ApiResponse<List<SubmissionReviewView>> list(@RequestParam Long assignmentId,
                                                        Authentication authentication) {
        SysUser user = currentUser(authService, authentication);
        authzService.requireRoleIn(user, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return ApiResponse.ok(reviewService.listByAssignmentId(assignmentId, user));
    }

    @GetMapping("/summary")
    public ApiResponse<SubmissionReviewSummaryView> summary(@RequestParam Long assignmentId,
                                                            Authentication authentication) {
        SysUser user = currentUser(authService, authentication);
        authzService.requireRoleIn(user, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return ApiResponse.ok(reviewService.summaryByAssignmentId(assignmentId, user));
    }

    @PutMapping("/rubric")
    public ApiResponse<AssignmentReviewRubricView> upsertRubric(@Valid @RequestBody AssignmentReviewRubricUpsertRequest request,
                                                                Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(reviewService.upsertRubric(request, user));
    }

    @GetMapping("/rubric")
    public ApiResponse<AssignmentReviewRubricView> rubric(@RequestParam Long assignmentId, Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        return ApiResponse.ok(reviewService.getRubric(assignmentId, user));
    }

    @GetMapping("/suggestion")
    public ApiResponse<ReviewSuggestionView> suggestion(@RequestParam(required = false) Long assignmentId,
                                                        @RequestParam(required = false) java.math.BigDecimal score,
                                                        Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        if (score == null) {
            score = java.math.BigDecimal.valueOf(60);
        }
        return ApiResponse.ok(reviewService.buildSuggestion(score));
    }

    @GetMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionReview> detail(@PathVariable Long submissionId,
                                                Authentication authentication) {
        SysUser user = currentUser(authService, authentication);
        authzService.requireRoleIn(user, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return ApiResponse.ok(reviewService.getBySubmissionId(submissionId, user));
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(@RequestParam Long assignmentId,
                                                    Authentication authentication) {
        SysUser user = requireTeacherOrAdmin(authService, authzService, authentication);
        byte[] csv = reviewService.exportReviewsCsv(assignmentId, user);
        ByteArrayResource resource = new ByteArrayResource(csv);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("assignment_" + assignmentId + "_reviews.csv").build()
        );
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentLength(csv.length);

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
