import axios from "axios";
import { buildApiUrl, buildAuthHeaders, getStoredToken } from "./config";
import http from "./http";

export type PlagiarismTaskPayload = {
  assignmentId: number;
  threshold: number;
  simhashWeight?: number;
  jaccardWeight?: number;
  idempotencyKey?: string;
  maxRetry?: number;
  runTimeoutSeconds?: number;
};

export type EvalCasePayload = {
  caseName: string;
  textA: string;
  textB: string;
  expectedRiskLevel: number;
  note?: string;
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
