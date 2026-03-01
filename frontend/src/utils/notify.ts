import { ElMessage } from "element-plus";

export function notifyApiError(error: unknown, fallback: string): void {
  if (typeof error === "string" && error.trim()) {
    ElMessage.error(error);
    return;
  }
  if (error instanceof Error && error.message) {
    ElMessage.error(error.message);
    return;
  }
  ElMessage.error(fallback);
}
