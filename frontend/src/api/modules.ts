import axios from "axios";
import { buildApiUrl, buildAuthHeaders, getStoredToken } from "./config";
import http from "./http";

type LoginPayload = {
  username: string;
  password: string;
};

type CoursePayload = {
  courseCode: string;
  courseName: string;
  teacherId: number;
  semester: string;
};

type AssignmentPayload = {
  courseId: number;
  title: string;
  description: string;
  deadline: string;
  maxScore: number;
  createdBy: number;
};

type SubmissionPayload = {
  assignmentId: number;
  studentId: number;
  fileId?: number;
  rawText?: string;
};

type SubmissionReviewPayload = {
  submissionId: number;
  score?: number;
  dimensionScores?: Array<{
    dimension: string;
    score: number;
    comment?: string;
  }>;
  comment?: string;
};

type PlagiarismTaskPayload = {
  assignmentId: number;
  threshold: number;
  simhashWeight?: number;
  jaccardWeight?: number;
  idempotencyKey?: string;
  maxRetry?: number;
  runTimeoutSeconds?: number;
};

type EvalCasePayload = {
  caseName: string;
  textA: string;
  textB: string;
  expectedRiskLevel: number;
  note?: string;
};

type PerfBaselinePayload = {
  baseUrl: string;
  path: string;
  requests: number;
  success: number;
  failed: number;
  errorRate: number;
  minMs: number;
  avgMs: number;
  p95Ms: number;
  maxMs: number;
  generatedAt?: string;
};

export const loginApi = (payload: LoginPayload) => http.post("/auth/login", payload);
export const meApi = () => http.get("/auth/me");

export const listCoursesApi = () => http.get("/courses");
export const createCourseApi = (payload: CoursePayload) => http.post("/courses", payload);

export const listAssignmentsApi = (courseId: number) =>
  http.get("/assignments", { params: { courseId } });
export const createAssignmentApi = (payload: AssignmentPayload) =>
  http.post("/assignments", payload);

export const uploadFileApi = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/files/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
};

export const createSubmissionApi = (payload: SubmissionPayload) =>
  http.post("/submissions", payload);

export const listSubmissionsApi = (assignmentId: number) =>
  http.get("/submissions", { params: { assignmentId } });

export const submissionEvolutionApi = (assignmentId: number, studentId: number) =>
  http.get("/submissions/evolution", { params: { assignmentId, studentId } });

export const upsertSubmissionReviewApi = (payload: SubmissionReviewPayload) =>
  http.post("/reviews", payload);

export const listSubmissionReviewsApi = (assignmentId: number) =>
  http.get("/reviews", { params: { assignmentId } });

export const submissionReviewSummaryApi = (assignmentId: number) =>
  http.get("/reviews/summary", { params: { assignmentId } });

export const upsertReviewRubricApi = (payload: {
  assignmentId: number;
  items: Array<{ dimension: string; weight: number; description?: string }>;
}) => http.put("/reviews/rubric", payload);

export const reviewRubricApi = (assignmentId: number) =>
  http.get("/reviews/rubric", { params: { assignmentId } });

export const reviewSuggestionApi = (params?: { assignmentId?: number; score?: number }) =>
  http.get("/reviews/suggestion", { params });

export const exportSubmissionReviewsCsvApi = (assignmentId: number) => {
  const token = getStoredToken();
  return axios.get(buildApiUrl("/reviews/export"), {
    params: { assignmentId },
    responseType: "blob",
    headers: buildAuthHeaders(token),
  });
};

export const createPlagiarismTaskApi = (payload: PlagiarismTaskPayload) =>
  http.post("/plagiarism/tasks", payload);

export const listPlagiarismTasksApi = (assignmentId: number) =>
  http.get("/plagiarism/tasks", { params: { assignmentId } });

export const latestPlagiarismTaskApi = (assignmentId: number) =>
  http.get("/plagiarism/tasks/latest", { params: { assignmentId } });

export const plagiarismTaskApi = (taskId: number) =>
  http.get(`/plagiarism/tasks/${taskId}`);

export const plagiarismTaskReportApi = (taskId: number) =>
  http.get(`/plagiarism/tasks/${taskId}/report`);

export const plagiarismTaskLogsApi = (taskId: number) =>
  http.get(`/plagiarism/tasks/${taskId}/logs`);

export const cancelPlagiarismTaskApi = (taskId: number) =>
  http.patch(`/plagiarism/tasks/${taskId}/cancel`);

export const retryPlagiarismTaskApi = (taskId: number) =>
  http.post(`/plagiarism/tasks/${taskId}/retry`);

export const listPlagiarismPairsApi = (
  taskId: number,
  params?: { riskLevel?: number; minSimilarity?: number; pageNo?: number; pageSize?: number }
) => http.get(`/plagiarism/tasks/${taskId}/pairs`, { params });

export const plagiarismPairDetailApi = (pairId: number) =>
  http.get(`/plagiarism/pairs/${pairId}`);

export const exportPlagiarismPairsCsvApi = (
  taskId: number,
  params?: { riskLevel?: number; minSimilarity?: number }
) => {
  const token = getStoredToken();
  return axios.get(buildApiUrl(`/plagiarism/tasks/${taskId}/pairs/export`), {
    params,
    responseType: "blob",
    headers: buildAuthHeaders(token),
  });
};

export const exportAssignmentPlagiarismReportApi = (assignmentId: number) => {
  const token = getStoredToken();
  return axios.get(buildApiUrl(`/plagiarism/assignments/${assignmentId}/report/export`), {
    responseType: "blob",
    headers: buildAuthHeaders(token),
  });
};

export const plagiarismAssignmentTrendApi = (
  assignmentId: number,
  params?: { startAt?: string; endAt?: string; limit?: number }
) => http.get(`/plagiarism/assignments/${assignmentId}/trend`, { params });

export const createPlagiarismEvalCaseApi = (payload: EvalCasePayload) =>
  http.post("/plagiarism/evaluation/cases", payload);

export const listPlagiarismEvalCasesApi = (enabled?: number) =>
  http.get("/plagiarism/evaluation/cases", { params: enabled === undefined ? undefined : { enabled } });

export const runPlagiarismEvaluationApi = (params?: {
  threshold?: number;
  simhashWeight?: number;
  jaccardWeight?: number;
}) => http.post("/plagiarism/evaluation/run", null, { params });

export const plagiarismEvaluationReportApi = () =>
  http.get("/plagiarism/evaluation/report");

export const listPlagiarismEvalRunsApi = (limit?: number) =>
  http.get("/plagiarism/evaluation/runs", { params: limit ? { limit } : undefined });

export const listAuditLogsApi = (params?: { actorUsername?: string; action?: string; limit?: number }) =>
  http.get("/audit/logs", { params });

export const listNotificationsApi = (params?: { status?: number; limit?: number }) =>
  http.get("/notifications", { params });

export const markNotificationsReadApi = (payload: { all: boolean; ids?: number[] }) =>
  http.post("/notifications/read", payload);

export const analyticsOverviewApi = () => http.get("/analytics/overview");

export const systemMetricsApi = () => http.get("/analytics/system-metrics");

export const listPerfBaselinesApi = (limit?: number) =>
  http.get("/perf/baselines", { params: limit ? { limit } : undefined });

export const createPerfBaselineApi = (payload: PerfBaselinePayload) =>
  http.post("/perf/baselines", payload);
