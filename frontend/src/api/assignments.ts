import http from "./http";

export type AssignmentPayload = {
  courseId: number;
  title: string;
  description: string;
  deadline: string;
  maxScore: number;
  createdBy: number;
};

export const listAssignmentsApi = (courseId: number) =>
  http.get("/assignments", { params: { courseId } });
export const createAssignmentApi = (payload: AssignmentPayload) =>
  http.post("/assignments", payload);
