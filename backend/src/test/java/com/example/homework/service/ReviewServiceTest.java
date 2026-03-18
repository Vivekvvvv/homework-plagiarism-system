package com.example.homework.service;

import com.example.homework.domain.vo.ReviewSuggestionView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.homework.mapper.AssignmentReviewRubricMapper;
import com.example.homework.mapper.SubmissionReviewMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private SubmissionReviewMapper submissionReviewMapper;
    @Mock
    private SubmissionService submissionService;
    @Mock
    private AssignmentService assignmentService;
    @Mock
    private AssignmentReviewRubricMapper assignmentReviewRubricMapper;
    @Mock
    private AuthzService authzService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
            submissionReviewMapper, submissionService, assignmentService,
            assignmentReviewRubricMapper, new ObjectMapper(),
            authzService, auditLogService, notificationService);
    }

    // --- buildSuggestion ---

    @Test
    void buildSuggestionShouldReturn优秀ForScoreAbove90() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(95));

        assertEquals("优秀", result.getLevel());
        assertNotNull(result.getSuggestion());
        assertTrue(result.getSuggestion().contains("结构完整"));
    }

    @Test
    void buildSuggestionShouldReturn良好ForScoreBetween80And89() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(85));

        assertEquals("良好", result.getLevel());
        assertNotNull(result.getSuggestion());
    }

    @Test
    void buildSuggestionShouldReturn及格ForScoreBetween60And79() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(65));

        assertEquals("及格", result.getLevel());
        assertNotNull(result.getSuggestion());
    }

    @Test
    void buildSuggestionShouldReturn需改进ForScoreBelow60() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(45));

        assertEquals("需改进", result.getLevel());
        assertNotNull(result.getSuggestion());
    }

    @Test
    void buildSuggestionShouldHandleNullScore() {
        ReviewSuggestionView result = reviewService.buildSuggestion(null);

        assertEquals("需改进", result.getLevel());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getScore()));
    }

    @Test
    void buildSuggestionShouldHandleBoundary90() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(90));

        assertEquals("优秀", result.getLevel());
    }

    @Test
    void buildSuggestionShouldHandleBoundary80() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(80));

        assertEquals("良好", result.getLevel());
    }

    @Test
    void buildSuggestionShouldHandleBoundary60() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(60));

        assertEquals("及格", result.getLevel());
    }

    @Test
    void buildSuggestionShouldHandleZeroScore() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.ZERO);

        assertEquals("需改进", result.getLevel());
    }

    @Test
    void buildSuggestionShouldHandleMaxScore() {
        ReviewSuggestionView result = reviewService.buildSuggestion(BigDecimal.valueOf(100));

        assertEquals("优秀", result.getLevel());
    }
}
