import http from "./http";

export type PerfBaselinePayload = {
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

export const analyticsOverviewApi = () => http.get("/analytics/overview");

export const systemMetricsApi = () => http.get("/analytics/system-metrics");

export const submissionTrendApi = (days?: number) =>
  http.get("/analytics/submission-trend", { params: days ? { days } : undefined });

export const listPerfBaselinesApi = (limit?: number) =>
  http.get("/perf/baselines", { params: limit ? { limit } : undefined });

export const createPerfBaselineApi = (payload: PerfBaselinePayload) =>
  http.post("/perf/baselines", payload);
