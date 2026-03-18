import http from "./http";

export type SubmissionPayload = {
  assignmentId: number;
  studentId: number;
  fileId?: number;
  rawText?: string;
};

const MAX_UPLOAD_SIZE = 10 * 1024 * 1024; // 10MB

export const uploadFileApi = (file: File) => {
  if (file.size > MAX_UPLOAD_SIZE) {
    return Promise.reject(new Error("文件大小不能超过 10MB，请压缩后重试"));
  }
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
