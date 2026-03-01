package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.domain.dto.CourseCreateRequest;
import com.example.homework.domain.entity.Course;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.service.AuthService;
import com.example.homework.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.homework.controller.ControllerSupport.currentUser;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final AuthService authService;

    public CourseController(CourseService courseService, AuthService authService) {
        this.courseService = courseService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<Course> create(@Valid @RequestBody CourseCreateRequest request, Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(courseService.create(request, actor));
    }

    @GetMapping
    public ApiResponse<List<Course>> listAll(Authentication authentication) {
        SysUser actor = currentUser(authService, authentication);
        return ApiResponse.ok(courseService.listAll(actor));
    }
}
