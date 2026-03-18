import axios from "axios";
import { buildApiUrl, buildAuthHeaders, getStoredToken } from "./config";
import http from "./http";

export type SubmissionReviewPayload = {
  submissionId: number;
  score?: number;
  dimensionScores?: Array<{
    dimension: string;
    score: number;
    comment?: string;
  }>;
  comment?: string;
};

export const upsertSubmissionReviewApi = (payload: SubmissionReviewPayload) =>
  http.post("/reviews", payload);

export const batchUpsertReviewApi = (
  payload: Array<SubmissionReviewPayload>
) => http.post("/reviews/batch", payload);

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
