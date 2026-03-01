type UserLike = {
  realName?: string | null;
  username?: string | null;
  role?: string | null;
};

type DashboardRole = "teacher" | "student";

export type DashboardCourse = {
  id: number;
  courseCode?: string | null;
  courseName?: string | null;
  semester?: string | null;
};

export type DashboardAssignment = {
  id: number;
  courseId: number;
  title?: string | null;
  deadline?: string | null;
  maxScore?: number | null;
};

export type DashboardReviewSummary = {
  totalSubmissions?: number | string | null;
  reviewedSubmissions?: number | string | null;
  reviewedRate?: number | string | null;
  averageScore?: number | string | null;
  passRate?: number | string | null;
};

export type DashboardPlagiarismTask = {
  status?: number | null;
  totalPairs?: number | string | null;
  highRiskPairs?: number | string | null;
};

export type DashboardSubmission = {
  id: number;
  assignmentId?: number | null;
  studentId?: number | null;
  versionNo?: number | string | null;
  sourceType?: number | string | null;
  tokenCount?: number | string | null;
  submitTime?: string | null;
};

export type DashboardSubmissionReview = {
  submissionId: number;
  assignmentId?: number | null;
  studentId?: number | null;
  score?: number | string | null;
  comment?: string | null;
  autoComment?: string | null;
  reviewedAt?: string | null;
};

type TeacherAssignmentInsight = {
  assignmentId: number;
  courseId: number;
  courseName: string;
  courseCode: string;
  semester: string;
  title: string;
  deadline: string;
  deadlineLabel: string;
  isOverdue: boolean;
  daysLeft: number | null;
  maxScore: number | null;
  submissionCount: number;
  reviewedCount: number;
  pendingReviewCount: number;
  reviewedRate: number;
  averageScore: number | null;
  passRate: number | null;
  highRiskPairs: number;
  totalPairs: number;
  plagiarismStatus: number | null;
  plagiarismStatusText: string;
  warningText: string;
};

type TeacherCourseInsight = {
  courseId: number;
  courseName: string;
  courseCode: string;
  semester: string;
  assignmentCount: number;
  submissionCount: number;
  highRiskAssignments: number;
};

type TeacherDashboardAlert = {
  assignmentId: number;
  courseId: number;
  title: string;
  type: "danger" | "warning" | "info";
  message: string;
  target: "assignments" | "reviews" | "plagiarism";
};

export type TeacherDashboardTodo = {
  assignmentId: number;
  courseId: number;
  title: string;
  courseName: string;
  deadlineLabel: string;
  urgency: "danger" | "warning" | "info";
  target: "assignments" | "reviews" | "plagiarism";
  actionText: string;
  reasons: string[];
  priority: number;
};

type TeacherDashboardSnapshot = {
  courseCount: number;
  assignmentCount: number;
  submissionCount: number;
  reviewedSubmissionCount: number;
  reviewCoverageRate: number;
  averageScore: number | null;
  overdueAssignments: number;
  upcomingAssignments: number;
  highRiskAssignments: number;
  focusAssignments: TeacherAssignmentInsight[];
  todos: TeacherDashboardTodo[];
  courseSummaries: TeacherCourseInsight[];
  alerts: TeacherDashboardAlert[];
};

type StudentAssignmentInsight = {
  assignmentId: number;
  courseId: number;
  courseName: string;
  courseCode: string;
  semester: string;
  title: string;
  deadline: string;
  deadlineLabel: string;
  isOverdue: boolean;
  daysLeft: number | null;
  submitted: boolean;
  latestSubmissionId: number | null;
  latestVersionNo: number | null;
  latestTokenCount: number;
  latestSubmitTime: string;
  latestSubmitTimeLabel: string;
  latestScore: number | null;
  latestReviewedAt: string;
  latestReviewedAtLabel: string;
  feedbackText: string;
  status: "pending_submission" | "awaiting_review" | "reviewed" | "overdue";
  statusText: string;
  actionText: string;
};

type StudentDashboardSnapshot = {
  courseCount: number;
  assignmentCount: number;
  submittedAssignmentCount: number;
  reviewedAssignmentCount: number;
  awaitingReviewCount: number;
  pendingAssignmentCount: number;
  overduePendingCount: number;
  latestAverageScore: number | null;
  todoAssignments: StudentAssignmentInsight[];
  feedbackAssignments: StudentAssignmentInsight[];
  recentAssignments: StudentAssignmentInsight[];
};

type TeacherDashboardInput = {
  courses?: DashboardCourse[];
  assignments?: DashboardAssignment[];
  submissionsByAssignment?: Record<number, unknown[] | undefined>;
  reviewSummaryByAssignment?: Record<number, DashboardReviewSummary | null | undefined>;
  plagiarismTaskByAssignment?: Record<number, DashboardPlagiarismTask | null | undefined>;
  focusAssignmentId?: number | null;
  now?: Date | string | number;
};

type StudentDashboardInput = {
  courses?: DashboardCourse[];
  assignments?: DashboardAssignment[];
  submissionsByAssignment?: Record<number, DashboardSubmission[] | undefined>;
  reviewsByAssignment?: Record<number, DashboardSubmissionReview[] | undefined>;
  focusAssignmentId?: number | null;
  now?: Date | string | number;
};

const DAY_MS = 24 * 60 * 60 * 1000;
const TEACHER_ROLES = new Set(["ADMIN", "TEACHER"]);

export function resolveDashboardUserName(user?: UserLike | null): string {
  return user?.realName || user?.username || "-";
}

export function resolveCoursesCount(data?: unknown): number {
  return Array.isArray(data) ? data.length : 0;
}

export function resolveDashboardStatus(): string {
  return "运行中";
}

export function resolveDashboardRole(role?: string | null): DashboardRole {
  return TEACHER_ROLES.has((role || "").trim().toUpperCase()) ? "teacher" : "student";
}

function toNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function toInteger(value: unknown, fallback = 0): number {
  const parsed = toNumber(value);
  if (parsed === null) {
    return fallback;
  }
  return Math.max(0, Math.round(parsed));
}

function normalizePercent(value: unknown): number | null {
  const parsed = toNumber(value);
  if (parsed === null) {
    return null;
  }
  const normalized = parsed > 0 && parsed <= 1 ? parsed * 100 : parsed;
  return Number(normalized.toFixed(2));
}

function normalizeAverageScore(value: unknown): number | null {
  const parsed = toNumber(value);
  if (parsed === null) {
    return null;
  }
  return Number(parsed.toFixed(2));
}

function normalizeDate(value?: string | null): Date | null {
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function resolveDeadlineLabel(deadline?: string | null): string {
  if (!deadline) {
    return "-";
  }
  const parsed = normalizeDate(deadline);
  if (!parsed) {
    return deadline;
  }
  const year = parsed.getFullYear();
  const month = `${parsed.getMonth() + 1}`.padStart(2, "0");
  const day = `${parsed.getDate()}`.padStart(2, "0");
  const hour = `${parsed.getHours()}`.padStart(2, "0");
  const minute = `${parsed.getMinutes()}`.padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function resolveDaysLeft(deadline?: string | null, now = new Date()): number | null {
  const parsed = normalizeDate(deadline);
  if (!parsed) {
    return null;
  }
  return Math.ceil((parsed.getTime() - now.getTime()) / DAY_MS);
}

function resolvePlagiarismStatusText(status?: number | null): string {
  if (status === 2) return "已完成";
  if (status === 1) return "运行中";
  if (status === 3) return "失败";
  if (status === 4) return "已取消";
  if (status === 0) return "待执行";
  return "未执行";
}

function resolveAssignmentWarning(assignment: {
  highRiskPairs: number;
  isOverdue: boolean;
  submissionCount: number;
  reviewedRate: number;
  daysLeft: number | null;
  plagiarismStatus: number | null;
}): string {
  if (assignment.highRiskPairs > 0) {
    return `查重高风险 ${assignment.highRiskPairs} 对`;
  }
  if (assignment.isOverdue && assignment.submissionCount > 0 && assignment.reviewedRate < 100) {
    return "已截止，仍有待评阅提交";
  }
  if (assignment.plagiarismStatus === 1) {
    return "查重任务仍在运行";
  }
  if (assignment.daysLeft !== null && assignment.daysLeft >= 0 && assignment.daysLeft <= 3) {
    return "临近截止，请关注提交进度";
  }
  return "进度正常";
}

function compareAssignments(a: TeacherAssignmentInsight, b: TeacherAssignmentInsight): number {
  if (a.highRiskPairs !== b.highRiskPairs) {
    return b.highRiskPairs - a.highRiskPairs;
  }
  if (a.isOverdue !== b.isOverdue) {
    return a.isOverdue ? -1 : 1;
  }
  const daysLeftA = a.daysLeft === null ? Number.MAX_SAFE_INTEGER : Math.abs(a.daysLeft);
  const daysLeftB = b.daysLeft === null ? Number.MAX_SAFE_INTEGER : Math.abs(b.daysLeft);
  if (daysLeftA !== daysLeftB) {
    return daysLeftA - daysLeftB;
  }
  if (a.submissionCount !== b.submissionCount) {
    return b.submissionCount - a.submissionCount;
  }
  return a.assignmentId - b.assignmentId;
}

function compareTodos(a: TeacherDashboardTodo, b: TeacherDashboardTodo): number {
  if (a.priority !== b.priority) {
    return b.priority - a.priority;
  }
  return a.assignmentId - b.assignmentId;
}

function compareStudentTodoAssignments(a: StudentAssignmentInsight, b: StudentAssignmentInsight): number {
  const priorityMap: Record<StudentAssignmentInsight["status"], number> = {
    overdue: 4,
    pending_submission: 3,
    awaiting_review: 2,
    reviewed: 1,
  };
  if (priorityMap[a.status] !== priorityMap[b.status]) {
    return priorityMap[b.status] - priorityMap[a.status];
  }
  const daysLeftA = a.daysLeft === null ? Number.MAX_SAFE_INTEGER : Math.abs(a.daysLeft);
  const daysLeftB = b.daysLeft === null ? Number.MAX_SAFE_INTEGER : Math.abs(b.daysLeft);
  if (daysLeftA !== daysLeftB) {
    return daysLeftA - daysLeftB;
  }
  return a.assignmentId - b.assignmentId;
}

function compareStudentFeedbackAssignments(a: StudentAssignmentInsight, b: StudentAssignmentInsight): number {
  const reviewedAtA = normalizeDate(a.latestReviewedAt);
  const reviewedAtB = normalizeDate(b.latestReviewedAt);
  const timeA = reviewedAtA ? reviewedAtA.getTime() : 0;
  const timeB = reviewedAtB ? reviewedAtB.getTime() : 0;
  if (timeA !== timeB) {
    return timeB - timeA;
  }
  const scoreA = a.latestScore ?? -1;
  const scoreB = b.latestScore ?? -1;
  if (scoreA !== scoreB) {
    return scoreB - scoreA;
  }
  return a.assignmentId - b.assignmentId;
}

function compareStudentRecentAssignments(a: StudentAssignmentInsight, b: StudentAssignmentInsight): number {
  const overdueA = a.isOverdue ? 1 : 0;
  const overdueB = b.isOverdue ? 1 : 0;
  if (overdueA !== overdueB) {
    return overdueB - overdueA;
  }
  const daysLeftA = a.daysLeft === null ? Number.MAX_SAFE_INTEGER : Math.abs(a.daysLeft);
  const daysLeftB = b.daysLeft === null ? Number.MAX_SAFE_INTEGER : Math.abs(b.daysLeft);
  if (daysLeftA !== daysLeftB) {
    return daysLeftA - daysLeftB;
  }
  return a.assignmentId - b.assignmentId;
}

function prioritizeFocusedAssignment<T extends { assignmentId: number }>(
  rows: T[],
  focusAssignmentId: number | null,
  limit: number
): T[] {
  const nextRows = rows.slice(0, limit);
  if (!focusAssignmentId) {
    return nextRows;
  }
  const focusedRow = rows.find((row) => row.assignmentId === focusAssignmentId);
  const focusedIndex = nextRows.findIndex((row) => row.assignmentId === focusAssignmentId);
  if (focusedRow && focusedIndex >= 0) {
    nextRows.splice(focusedIndex, 1);
    nextRows.unshift(focusedRow);
    return nextRows;
  }
  if (focusedRow) {
    nextRows.pop();
    nextRows.unshift(focusedRow);
  }
  return nextRows;
}

export function formatDashboardPercent(value?: number | null): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return Math.abs(value % 1) < 0.01 ? value.toFixed(0) : value.toFixed(1);
}

export function formatDashboardScore(value?: number | null): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return Math.abs(value % 1) < 0.01 ? value.toFixed(0) : value.toFixed(1);
}

export function resolveDashboardHandledMessage(handled?: string | null, handledCount = 1): string | null {
  const count = handledCount > 0 ? handledCount : 1;
  if (handled === "review_saved") {
    return `已处理 ${count} 项待办：评阅已保存`;
  }
  if (handled === "plagiarism_started") {
    return `已处理 ${count} 项待办：查重任务已发起`;
  }
  if (handled === "assignment_checked") {
    return "已返回工作台：已查看作业详情";
  }
  return null;
}

function resolveStudentStatusText(status: StudentAssignmentInsight["status"]): string {
  if (status === "reviewed") {
    return "已评阅";
  }
  if (status === "awaiting_review") {
    return "待反馈";
  }
  if (status === "overdue") {
    return "已截止未提交";
  }
  return "待提交";
}

export function buildStudentDashboardSnapshot({
  courses = [],
  assignments = [],
  submissionsByAssignment = {},
  reviewsByAssignment = {},
  focusAssignmentId = null,
  now = new Date(),
}: StudentDashboardInput): StudentDashboardSnapshot {
  const currentTime = now instanceof Date ? now : new Date(now);
  const courseMap = new Map(courses.map((course) => [course.id, course]));

  const assignmentInsights = assignments.map<StudentAssignmentInsight>((assignment) => {
    const course = courseMap.get(assignment.courseId);
    const submissions = Array.isArray(submissionsByAssignment[assignment.id])
      ? [...(submissionsByAssignment[assignment.id] || [])]
      : [];
    submissions.sort((left, right) => {
      const versionDiff = toInteger(right.versionNo) - toInteger(left.versionNo);
      if (versionDiff !== 0) {
        return versionDiff;
      }
      const submitTimeDiff =
        (normalizeDate(right.submitTime)?.getTime() || 0) - (normalizeDate(left.submitTime)?.getTime() || 0);
      if (submitTimeDiff !== 0) {
        return submitTimeDiff;
      }
      return right.id - left.id;
    });

    const reviews = Array.isArray(reviewsByAssignment[assignment.id]) ? [...(reviewsByAssignment[assignment.id] || [])] : [];
    const reviewMap = new Map(
      reviews
        .filter((review) => review?.submissionId !== null && review?.submissionId !== undefined)
        .map((review) => [review.submissionId, review])
    );
    const reviewedRows = reviews
      .filter((review) => toNumber(review?.score) !== null)
      .sort((left, right) => {
        const reviewedAtDiff =
          (normalizeDate(right.reviewedAt)?.getTime() || 0) - (normalizeDate(left.reviewedAt)?.getTime() || 0);
        if (reviewedAtDiff !== 0) {
          return reviewedAtDiff;
        }
        return right.submissionId - left.submissionId;
      });

    const latestSubmission = submissions[0] || null;
    const latestSubmissionReview = latestSubmission ? reviewMap.get(latestSubmission.id) || null : null;
    const latestReviewed = reviewedRows[0] || null;
    const latestReviewedScore = normalizeAverageScore(latestSubmissionReview?.score ?? latestReviewed?.score);
    const daysLeft = resolveDaysLeft(assignment.deadline, currentTime);
    const isOverdue = daysLeft !== null && daysLeft < 0;
    const submitted = latestSubmission !== null;

    let status: StudentAssignmentInsight["status"];
    if (!submitted) {
      status = isOverdue ? "overdue" : "pending_submission";
    } else if (toNumber(latestSubmissionReview?.score) !== null) {
      status = "reviewed";
    } else {
      status = "awaiting_review";
    }

    let feedbackText = "已提交，等待教师评阅";
    if (status === "reviewed") {
      feedbackText = latestSubmissionReview?.comment || latestSubmissionReview?.autoComment || "已收到教师反馈";
    } else if (status === "awaiting_review" && latestReviewed) {
      feedbackText = "新版本待评阅，可参考上一版反馈";
    } else if (status === "pending_submission") {
      feedbackText =
        daysLeft !== null && daysLeft >= 0 && daysLeft <= 3
          ? daysLeft === 0
            ? "今天截止，建议尽快提交"
            : `${daysLeft} 天内截止，建议尽快提交`
          : "尚未提交本作业";
    } else if (status === "overdue") {
      feedbackText = "已截止且尚未提交，建议尽快联系教师";
    }

    return {
      assignmentId: assignment.id,
      courseId: assignment.courseId,
      courseName: course?.courseName || `课程 ${assignment.courseId}`,
      courseCode: course?.courseCode || "-",
      semester: course?.semester || "-",
      title: assignment.title || `作业 ${assignment.id}`,
      deadline: assignment.deadline || "",
      deadlineLabel: resolveDeadlineLabel(assignment.deadline),
      isOverdue,
      daysLeft,
      submitted,
      latestSubmissionId: latestSubmission?.id || null,
      latestVersionNo: latestSubmission ? toInteger(latestSubmission.versionNo, 1) : null,
      latestTokenCount: toInteger(latestSubmission?.tokenCount, 0),
      latestSubmitTime: latestSubmission?.submitTime || "",
      latestSubmitTimeLabel: resolveDeadlineLabel(latestSubmission?.submitTime || null),
      latestScore: latestReviewedScore,
      latestReviewedAt: latestSubmissionReview?.reviewedAt || latestReviewed?.reviewedAt || "",
      latestReviewedAtLabel: resolveDeadlineLabel(latestSubmissionReview?.reviewedAt || latestReviewed?.reviewedAt || null),
      feedbackText,
      status,
      statusText: resolveStudentStatusText(status),
      actionText: submitted ? "查看记录" : "去提交",
    };
  });

  const reviewedScores = assignmentInsights.map((item) => item.latestScore).filter((score): score is number => score !== null);

  return {
    courseCount: courses.length,
    assignmentCount: assignmentInsights.length,
    submittedAssignmentCount: assignmentInsights.filter((item) => item.submitted).length,
    reviewedAssignmentCount: assignmentInsights.filter((item) => item.status === "reviewed").length,
    awaitingReviewCount: assignmentInsights.filter((item) => item.status === "awaiting_review").length,
    pendingAssignmentCount: assignmentInsights.filter((item) => item.status === "pending_submission").length,
    overduePendingCount: assignmentInsights.filter((item) => item.status === "overdue").length,
    latestAverageScore:
      reviewedScores.length > 0
        ? Number((reviewedScores.reduce((sum, score) => sum + score, 0) / reviewedScores.length).toFixed(2))
        : null,
    todoAssignments: prioritizeFocusedAssignment(
      assignmentInsights.filter((item) => item.status !== "reviewed").sort(compareStudentTodoAssignments),
      focusAssignmentId,
      8
    ),
    feedbackAssignments: prioritizeFocusedAssignment(
      assignmentInsights
        .filter((item) => item.latestScore !== null || item.latestReviewedAtLabel !== "-")
        .sort(compareStudentFeedbackAssignments),
      focusAssignmentId,
      6
    ),
    recentAssignments: prioritizeFocusedAssignment(
      [...assignmentInsights].sort(compareStudentRecentAssignments),
      focusAssignmentId,
      8
    ),
  };
}

export function buildTeacherDashboardSnapshot({
  courses = [],
  assignments = [],
  submissionsByAssignment = {},
  reviewSummaryByAssignment = {},
  plagiarismTaskByAssignment = {},
  focusAssignmentId = null,
  now = new Date(),
}: TeacherDashboardInput): TeacherDashboardSnapshot {
  const currentTime = now instanceof Date ? now : new Date(now);
  const courseMap = new Map(courses.map((course) => [course.id, course]));
  const assignmentInsights = assignments
    .map<TeacherAssignmentInsight>((assignment) => {
      const course = courseMap.get(assignment.courseId);
      const submissionRows = submissionsByAssignment[assignment.id] || [];
      const reviewSummary = reviewSummaryByAssignment[assignment.id] || null;
      const plagiarismTask = plagiarismTaskByAssignment[assignment.id] || null;
      const rawSubmissionCount = Array.isArray(submissionRows) ? submissionRows.length : 0;
      const totalSubmissions = Math.max(rawSubmissionCount, toInteger(reviewSummary?.totalSubmissions, rawSubmissionCount));
      const reviewedCount = Math.min(totalSubmissions, toInteger(reviewSummary?.reviewedSubmissions, 0));
      const reviewedRate =
        totalSubmissions > 0
          ? Number(((reviewedCount / totalSubmissions) * 100).toFixed(2))
          : normalizePercent(reviewSummary?.reviewedRate) || 0;
      const pendingReviewCount = Math.max(0, totalSubmissions - reviewedCount);
      const averageScore = normalizeAverageScore(reviewSummary?.averageScore);
      const passRate = normalizePercent(reviewSummary?.passRate);
      const highRiskPairs = toInteger(plagiarismTask?.highRiskPairs, 0);
      const totalPairs = toInteger(plagiarismTask?.totalPairs, 0);
      const daysLeft = resolveDaysLeft(assignment.deadline, currentTime);
      const isOverdue = daysLeft !== null && daysLeft < 0;
      const plagiarismStatus = toNumber(plagiarismTask?.status);

      const row: TeacherAssignmentInsight = {
        assignmentId: assignment.id,
        courseId: assignment.courseId,
        courseName: course?.courseName || `课程 ${assignment.courseId}`,
        courseCode: course?.courseCode || "-",
        semester: course?.semester || "-",
        title: assignment.title || `作业 ${assignment.id}`,
        deadline: assignment.deadline || "",
        deadlineLabel: resolveDeadlineLabel(assignment.deadline),
        isOverdue,
        daysLeft,
        maxScore: toNumber(assignment.maxScore),
        submissionCount: totalSubmissions,
        reviewedCount,
        pendingReviewCount,
        reviewedRate,
        averageScore,
        passRate,
        highRiskPairs,
        totalPairs,
        plagiarismStatus,
        plagiarismStatusText: resolvePlagiarismStatusText(plagiarismStatus),
        warningText: "",
      };
      row.warningText = resolveAssignmentWarning(row);
      return row;
    })
    .sort(compareAssignments);

  let reviewedScoreWeight = 0;
  let reviewedScoreTotal = 0;
  for (const row of assignmentInsights) {
    if (row.averageScore !== null && row.reviewedCount > 0) {
      reviewedScoreWeight += row.reviewedCount;
      reviewedScoreTotal += row.averageScore * row.reviewedCount;
    }
  }

  const courseSummaries = courses
    .map<TeacherCourseInsight>((course) => {
      const relatedAssignments = assignmentInsights.filter((item) => item.courseId === course.id);
      return {
        courseId: course.id,
        courseName: course.courseName || `课程 ${course.id}`,
        courseCode: course.courseCode || "-",
        semester: course.semester || "-",
        assignmentCount: relatedAssignments.length,
        submissionCount: relatedAssignments.reduce((sum, item) => sum + item.submissionCount, 0),
        highRiskAssignments: relatedAssignments.filter((item) => item.highRiskPairs > 0).length,
      };
    })
    .sort((a, b) => {
      if (a.assignmentCount !== b.assignmentCount) {
        return b.assignmentCount - a.assignmentCount;
      }
      if (a.submissionCount !== b.submissionCount) {
        return b.submissionCount - a.submissionCount;
      }
      return a.courseId - b.courseId;
    });

  const alerts = assignmentInsights
    .filter((item) => item.warningText !== "进度正常")
    .slice(0, 6)
    .map<TeacherDashboardAlert>((item) => ({
      assignmentId: item.assignmentId,
      courseId: item.courseId,
      title: item.title,
      type: item.highRiskPairs > 0 ? "danger" : item.isOverdue ? "warning" : "info",
      message: `${item.courseName}：${item.warningText}`,
      target: item.highRiskPairs > 0 ? "plagiarism" : item.isOverdue ? "reviews" : "assignments",
    }));

  const todos = assignmentInsights
    .map<TeacherDashboardTodo | null>((item) => {
      const reasons: string[] = [];
      let priority = 0;
      let urgency: TeacherDashboardTodo["urgency"] = "info";
      let target: TeacherDashboardTodo["target"] = "assignments";
      let actionText = "查看作业";

      if (item.highRiskPairs > 0) {
        reasons.push(`查重高风险 ${item.highRiskPairs} 对`);
        priority += 100 + item.highRiskPairs;
        urgency = "danger";
        target = "plagiarism";
        actionText = "处理查重";
      }

      if (item.isOverdue && item.pendingReviewCount > 0) {
        reasons.push(`已截止，仍有 ${item.pendingReviewCount} 份待评阅`);
        priority += 80 + item.pendingReviewCount;
        if (target !== "plagiarism") {
          urgency = "warning";
          target = "reviews";
          actionText = "连续评阅";
        }
      } else if (item.pendingReviewCount > 0) {
        reasons.push(`还有 ${item.pendingReviewCount} 份提交未评阅`);
        priority += 50 + item.pendingReviewCount;
        if (target === "assignments") {
          target = "reviews";
          actionText = "连续评阅";
        }
      }

      if (item.plagiarismStatus === 1) {
        reasons.push("查重任务运行中");
        priority += 35;
        if (target === "assignments") {
          target = "plagiarism";
          actionText = "查看进度";
        }
      }

      if (item.daysLeft !== null && item.daysLeft >= 0 && item.daysLeft <= 3) {
        reasons.push(item.daysLeft === 0 ? "今天截止" : `${item.daysLeft} 天内截止`);
        priority += 20 + (3 - item.daysLeft) * 5;
        if (target === "assignments") {
          target = "assignments";
          actionText = "查看作业";
        }
      }

      if (reasons.length === 0) {
        return null;
      }

      return {
        assignmentId: item.assignmentId,
        courseId: item.courseId,
        title: item.title,
        courseName: item.courseName,
        deadlineLabel: item.deadlineLabel,
        urgency,
        target,
        actionText,
        reasons,
        priority,
      };
    })
    .filter((item): item is TeacherDashboardTodo => item !== null)
    .sort(compareTodos)
    .slice(0, 8);

  const submissionCount = assignmentInsights.reduce((sum, item) => sum + item.submissionCount, 0);
  const reviewedSubmissionCount = assignmentInsights.reduce((sum, item) => sum + item.reviewedCount, 0);

  const topAssignments = assignmentInsights.slice(0, 8);

  return {
    courseCount: courses.length,
    assignmentCount: assignmentInsights.length,
    submissionCount,
    reviewedSubmissionCount,
    reviewCoverageRate:
      submissionCount > 0 ? Number(((reviewedSubmissionCount / submissionCount) * 100).toFixed(2)) : 0,
    averageScore:
      reviewedScoreWeight > 0 ? Number((reviewedScoreTotal / reviewedScoreWeight).toFixed(2)) : null,
    overdueAssignments: assignmentInsights.filter((item) => item.isOverdue).length,
    upcomingAssignments: assignmentInsights.filter((item) => item.daysLeft !== null && item.daysLeft >= 0).length,
    highRiskAssignments: assignmentInsights.filter((item) => item.highRiskPairs > 0).length,
    focusAssignments: prioritizeFocusedAssignment(topAssignments, focusAssignmentId, 8),
    todos,
    courseSummaries: courseSummaries.slice(0, 6),
    alerts,
  };
}
