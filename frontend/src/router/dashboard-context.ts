import { readStringQuery } from "./query";

type DashboardReturnOptions = {
  focusAssignmentId?: number | null;
  focusCourseId?: number | null;
  handled?: string;
  handledCount?: number;
};

export function isFromDashboard(query: Record<string, unknown>): boolean {
  return readStringQuery(query.from) === "dashboard";
}

export function buildDashboardReturnQuery(options: DashboardReturnOptions): Record<string, string> {
  return {
    from: "dashboard",
    ...(options.focusAssignmentId ? { focusAssignmentId: String(options.focusAssignmentId) } : {}),
    ...(options.focusCourseId ? { focusCourseId: String(options.focusCourseId) } : {}),
    ...(options.handled ? { handled: options.handled } : {}),
    ...(options.handledCount ? { handledCount: String(options.handledCount) } : {}),
  };
}
