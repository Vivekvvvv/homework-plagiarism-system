package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.dto.AssignmentCreateRequest;
import com.example.homework.domain.entity.Assignment;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.service.AuthService;
import com.example.homework.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.homework.controller.ControllerSupport.currentUser;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AuthService authService;

    public AssignmentController(AssignmentService assignmentService, AuthService authService) {
        this.assignmentService = assignmentService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<Assignment> create(@Valid @RequestBody AssignmentCreateRequest request,
                                          Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(assignmentService.create(request, actor));
    }

    @GetMapping
    public ApiResponse<List<Assignment>> listByCourse(@RequestParam Long courseId,
                                                      Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(assignmentService.listByCourseId(courseId, actor));
    }
}
