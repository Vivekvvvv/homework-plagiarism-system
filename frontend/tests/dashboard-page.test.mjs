import assert from "node:assert/strict";

import {
  buildStudentDashboardSnapshot,
  buildTeacherDashboardSnapshot,
  formatDashboardPercent,
  formatDashboardScore,
  resolveDashboardHandledMessage,
  resolveCoursesCount,
  resolveDashboardRole,
  resolveDashboardStatus,
  resolveDashboardUserName,
} from "../.test-dist/src/views/dashboard.logic.js";

export function runDashboardPageTests() {
  assert.equal(resolveDashboardUserName({ realName: "教师甲", username: "teacher1" }), "教师甲");
  assert.equal(resolveDashboardUserName({ username: "teacher1" }), "teacher1");
  assert.equal(resolveDashboardUserName(null), "-");

  assert.equal(resolveCoursesCount([{ id: 1 }, { id: 2 }]), 2);
  assert.equal(resolveCoursesCount(undefined), 0);
  assert.equal(resolveDashboardStatus(), "运行中");
  assert.equal(resolveDashboardRole("teacher"), "teacher");
  assert.equal(resolveDashboardRole("student"), "student");

  const snapshot = buildTeacherDashboardSnapshot({
    courses: [
      { id: 1, courseCode: "SE101", courseName: "软件工程", semester: "2025-2026-2" },
      { id: 2, courseCode: "DB201", courseName: "数据库", semester: "2025-2026-2" },
    ],
    assignments: [
      { id: 11, courseId: 1, title: "需求分析", deadline: "2026-03-12T10:00:00", maxScore: 100 },
      { id: 12, courseId: 1, title: "设计文档", deadline: "2026-03-01T10:00:00", maxScore: 100 },
      { id: 21, courseId: 2, title: "SQL实验", deadline: "2026-03-11T10:00:00", maxScore: 100 },
    ],
    submissionsByAssignment: {
      11: [{ id: 1 }, { id: 2 }, { id: 3 }],
      12: [{ id: 4 }, { id: 5 }],
      21: [],
    },
    reviewSummaryByAssignment: {
      11: { totalSubmissions: 3, reviewedSubmissions: 2, reviewedRate: 66.67, averageScore: 88, passRate: 1 },
      12: { totalSubmissions: 2, reviewedSubmissions: 1, reviewedRate: 0.5, averageScore: 72.5, passRate: 0.5 },
      21: { totalSubmissions: 0, reviewedSubmissions: 0, reviewedRate: 0, averageScore: null, passRate: null },
    },
    plagiarismTaskByAssignment: {
      11: { status: 2, totalPairs: 3, highRiskPairs: 1 },
      12: { status: 1, totalPairs: 0, highRiskPairs: 0 },
      21: null,
    },
    focusAssignmentId: 21,
    now: "2026-03-09T00:00:00",
  });

  assert.equal(snapshot.courseCount, 2);
  assert.equal(snapshot.assignmentCount, 3);
  assert.equal(snapshot.submissionCount, 5);
  assert.equal(snapshot.reviewedSubmissionCount, 3);
  assert.equal(snapshot.reviewCoverageRate, 60);
  assert.equal(snapshot.overdueAssignments, 1);
  assert.equal(snapshot.upcomingAssignments, 2);
  assert.equal(snapshot.highRiskAssignments, 1);
  assert.equal(snapshot.courseSummaries[0].courseId, 1);
  assert.equal(snapshot.courseSummaries[0].assignmentCount, 2);
  assert.equal(snapshot.courseSummaries[0].submissionCount, 5);
  assert.equal(snapshot.focusAssignments[0].assignmentId, 21);
  assert.equal(snapshot.focusAssignments[1].assignmentId, 11);
  assert.equal(snapshot.focusAssignments[1].pendingReviewCount, 1);
  assert.equal(snapshot.focusAssignments[1].warningText, "查重高风险 1 对");
  assert.equal(snapshot.focusAssignments[2].assignmentId, 12);
  assert.equal(snapshot.focusAssignments[2].pendingReviewCount, 1);
  assert.equal(snapshot.focusAssignments[2].warningText, "已截止，仍有待评阅提交");
  assert.equal(snapshot.todos.length, 3);
  assert.equal(snapshot.todos[0].assignmentId, 11);
  assert.equal(snapshot.todos[0].target, "plagiarism");
  assert.equal(snapshot.todos[0].actionText, "处理查重");
  assert.equal(snapshot.todos[1].assignmentId, 12);
  assert.equal(snapshot.todos[1].target, "reviews");
  assert.equal(snapshot.todos[1].actionText, "连续评阅");
  assert.equal(snapshot.todos[1].reasons[0], "已截止，仍有 1 份待评阅");
  assert.equal(snapshot.todos[2].assignmentId, 21);
  assert.equal(snapshot.todos[2].target, "assignments");
  assert.equal(snapshot.todos[2].reasons[0], "3 天内截止");
  assert.equal(snapshot.alerts.length, 3);
  assert.equal(snapshot.alerts[0].target, "plagiarism");
  assert.equal(snapshot.alerts[1].target, "reviews");
  assert.equal(snapshot.alerts[2].target, "assignments");
  assert.equal(snapshot.averageScore, 82.83);

  const studentSnapshot = buildStudentDashboardSnapshot({
    courses: [
      { id: 1, courseCode: "SE101", courseName: "软件工程", semester: "2025-2026-2" },
      { id: 2, courseCode: "DB201", courseName: "数据库", semester: "2025-2026-2" },
    ],
    assignments: [
      { id: 11, courseId: 1, title: "需求分析", deadline: "2026-03-12T10:00:00", maxScore: 100 },
      { id: 12, courseId: 1, title: "设计文档", deadline: "2026-03-01T10:00:00", maxScore: 100 },
      { id: 21, courseId: 2, title: "SQL实验", deadline: "2026-03-11T10:00:00", maxScore: 100 },
    ],
    submissionsByAssignment: {
      11: [{ id: 1001, assignmentId: 11, versionNo: 2, tokenCount: 520, submitTime: "2026-03-09T18:00:00" }],
      21: [{ id: 2001, assignmentId: 21, versionNo: 1, tokenCount: 260, submitTime: "2026-03-10T08:30:00" }],
    },
    reviewsByAssignment: {
      11: [
        {
          submissionId: 1001,
          assignmentId: 11,
          score: 91.5,
          comment: "结构完整，继续补充边界说明",
          reviewedAt: "2026-03-10T09:00:00",
        },
      ],
    },
    focusAssignmentId: 21,
    now: "2026-03-10T00:00:00",
  });

  assert.equal(studentSnapshot.courseCount, 2);
  assert.equal(studentSnapshot.assignmentCount, 3);
  assert.equal(studentSnapshot.submittedAssignmentCount, 2);
  assert.equal(studentSnapshot.reviewedAssignmentCount, 1);
  assert.equal(studentSnapshot.awaitingReviewCount, 1);
  assert.equal(studentSnapshot.pendingAssignmentCount, 0);
  assert.equal(studentSnapshot.overduePendingCount, 1);
  assert.equal(studentSnapshot.latestAverageScore, 91.5);
  assert.equal(studentSnapshot.todoAssignments[0].assignmentId, 21);
  assert.equal(studentSnapshot.todoAssignments[0].status, "awaiting_review");
  assert.equal(studentSnapshot.todoAssignments[1].assignmentId, 12);
  assert.equal(studentSnapshot.todoAssignments[1].status, "overdue");
  assert.equal(studentSnapshot.feedbackAssignments[0].assignmentId, 11);
  assert.equal(studentSnapshot.feedbackAssignments[0].status, "reviewed");
  assert.equal(studentSnapshot.feedbackAssignments[0].latestScore, 91.5);
  assert.equal(studentSnapshot.recentAssignments[0].assignmentId, 21);

  assert.equal(formatDashboardPercent(60), "60");
  assert.equal(formatDashboardPercent(66.67), "66.7");
  assert.equal(formatDashboardScore(82.83), "82.8");
  assert.equal(formatDashboardScore(null), "-");
  assert.equal(resolveDashboardHandledMessage("assignment_checked", 1), "已返回工作台：已查看作业详情");
  assert.equal(resolveDashboardHandledMessage("plagiarism_started", 1), "已处理 1 项待办：查重任务已发起");
  assert.equal(resolveDashboardHandledMessage("review_saved", 1), "已处理 1 项待办：评阅已保存");
  assert.equal(resolveDashboardHandledMessage("review_saved", 2), "已处理 2 项待办：评阅已保存");
  assert.equal(resolveDashboardHandledMessage("unknown", 1), null);
}
