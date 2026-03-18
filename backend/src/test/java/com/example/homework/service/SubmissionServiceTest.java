package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.dto.SubmissionCreateRequest;
import com.example.homework.domain.entity.*;
import com.example.homework.mapper.AssignmentMapper;
import com.example.homework.mapper.SubmissionMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import com.example.homework.mapper.SubmissionTextMapper;
import com.example.homework.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private SubmissionTextMapper submissionTextMapper;
    @Mock private SubmissionReviewMapper submissionReviewMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private TextExtractService textExtractService;
    @Mock private CourseService courseService;
    @Mock private AuthService authService;
    @Mock private AuthzService authzService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(
            submissionMapper, submissionTextMapper, submissionReviewMapper,
            assignmentMapper, fileStorageService, textExtractService,
            courseService, authService, authzService, auditLogService,
            notificationService);
    }

    // --- create: text submission ---

    @Test
    @SuppressWarnings("unchecked")
    void createShouldSaveTextSubmission() {
        SysUser student = buildUser(5L, UserRole.STUDENT);
        Assignment assignment = buildAssignment(10L, 1L);
        Course course = new Course();
        course.setTeacherId(2L);

        when(assignmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assignment);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null); // first version
        when(submissionMapper.insert(any(Submission.class))).thenReturn(1);
        when(submissionTextMapper.insert(any(SubmissionText.class))).thenReturn(1);
        when(courseService.getById(1L)).thenReturn(course);

        SubmissionCreateRequest request = new SubmissionCreateRequest();
        request.setAssignmentId(10L);
        request.setStudentId(5L);
        request.setRawText("Hello world essay");

        Submission result = submissionService.create(request, student);

        assertNotNull(result);
        assertEquals(10L, result.getAssignmentId());
        assertEquals(5L, result.getStudentId());
        assertEquals(2, result.getSourceType()); // text source
        assertEquals(1, result.getVersionNo());
        verify(submissionMapper).insert(any(Submission.class));
        verify(submissionTextMapper).insert(any(SubmissionText.class));
        verify(auditLogService).log(any(), any(), any(), any(), any(), any(), any());
    }

    // --- create: file submission ---

    @Test
    @SuppressWarnings("unchecked")
    void createShouldSaveFileSubmission() {
        SysUser student = buildUser(5L, UserRole.STUDENT);
        Assignment assignment = buildAssignment(10L, 1L);
        Course course = new Course();
        course.setTeacherId(2L);
        FileStorage fileStorage = new FileStorage();
        fileStorage.setFileName("test.txt");
        fileStorage.setMimeType("text/plain");

        when(assignmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assignment);
        when(fileStorageService.getById(100L)).thenReturn(fileStorage);
        when(fileStorageService.readFileBytes(fileStorage)).thenReturn("extracted text".getBytes());
        when(textExtractService.extractTextFromBytes("test.txt", "text/plain", "extracted text".getBytes()))
            .thenReturn("extracted text");
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(submissionMapper.insert(any(Submission.class))).thenReturn(1);
        when(submissionTextMapper.insert(any(SubmissionText.class))).thenReturn(1);
        when(courseService.getById(1L)).thenReturn(course);

        SubmissionCreateRequest request = new SubmissionCreateRequest();
        request.setAssignmentId(10L);
        request.setStudentId(5L);
        request.setFileId(100L);

        Submission result = submissionService.create(request, student);

        assertNotNull(result);
        assertEquals(1, result.getSourceType()); // file source
        verify(fileStorageService).getById(100L);
        verify(textExtractService).extractTextFromBytes(any(), any(), any());
    }

    // --- create: both empty should throw ---

    @Test
    @SuppressWarnings("unchecked")
    void createShouldThrowWhenBothFileAndTextEmpty() {
        SysUser student = buildUser(5L, UserRole.STUDENT);
        Assignment assignment = buildAssignment(10L, 1L);

        when(assignmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assignment);

        SubmissionCreateRequest request = new SubmissionCreateRequest();
        request.setAssignmentId(10L);
        request.setStudentId(5L);
        // no fileId and no rawText

        BusinessException ex = assertThrows(BusinessException.class,
            () -> submissionService.create(request, student));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能同时为空"));
    }

    // --- create: assignment not found ---

    @Test
    @SuppressWarnings("unchecked")
    void createShouldThrowWhenAssignmentNotFound() {
        SysUser student = buildUser(5L, UserRole.STUDENT);

        when(assignmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SubmissionCreateRequest request = new SubmissionCreateRequest();
        request.setAssignmentId(999L);
        request.setStudentId(5L);
        request.setRawText("Some text");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> submissionService.create(request, student));
        assertEquals(404, ex.getCode());
    }

    // --- requireSubmissionAccessible ---

    @Test
    @SuppressWarnings("unchecked")
    void requireSubmissionAccessibleShouldReturnSubmission() {
        SysUser admin = buildUser(1L, UserRole.ADMIN);
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setAssignmentId(10L);
        submission.setStudentId(5L);
        Assignment assignment = buildAssignment(10L, 1L);

        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(submission);
        when(assignmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assignment);
        when(authService.isAdmin(admin)).thenReturn(true);

        Submission result = submissionService.requireSubmissionAccessible(1L, admin);

        assertEquals(1L, result.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void requireSubmissionAccessibleShouldThrowWhenNotFound() {
        SysUser admin = buildUser(1L, UserRole.ADMIN);

        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> submissionService.requireSubmissionAccessible(999L, admin));
        assertEquals(404, ex.getCode());
    }

    // --- listByAssignmentId ---

    @Test
    @SuppressWarnings("unchecked")
    void listByAssignmentIdShouldReturnViews() {
        SysUser admin = buildUser(1L, UserRole.ADMIN);
        Assignment assignment = buildAssignment(10L, 1L);
        Submission sub = new Submission();
        sub.setId(1L);
        sub.setAssignmentId(10L);
        sub.setStudentId(5L);
        sub.setVersionNo(1);
        sub.setSourceType(2);
        sub.setContentHash("abc123");

        when(assignmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assignment);
        when(authService.isAdmin(admin)).thenReturn(true);
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub));
        when(authService.isStudent(admin)).thenReturn(false);
        when(submissionTextMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var result = submissionService.listByAssignmentId(10L, admin);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getAssignmentId());
    }

    // --- helpers ---

    private SysUser buildUser(Long id, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        return user;
    }

    private Assignment buildAssignment(Long id, Long courseId) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setCourseId(courseId);
        assignment.setTitle("Test Assignment");
        return assignment;
    }
}
