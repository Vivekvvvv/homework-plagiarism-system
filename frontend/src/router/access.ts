const TEACHER_ROLES = new Set(["ADMIN", "TEACHER"]);

export const teacherOnlyPaths = [
  "/courses",
  "/assignments",
  "/plagiarism",
  "/reviews",
  "/evaluation",
  "/analytics",
] as const;

type RouteAccessContext = {
  toPath: string;
  token?: string | null;
  role?: string | null;
};

export function normalizeRole(role?: string | null): string {
  return (role || "").trim().toUpperCase();
}

export function resolveRouteRedirect({ toPath, token, role }: RouteAccessContext): string | null {
  if (toPath !== "/login" && !token) {
    return "/login";
  }

  const normalizedRole = normalizeRole(role);
  if (
    teacherOnlyPaths.includes(toPath as (typeof teacherOnlyPaths)[number]) &&
    normalizedRole &&
    !TEACHER_ROLES.has(normalizedRole)
  ) {
    return "/submissions";
  }

  if (toPath === "/login" && token) {
    return "/dashboard";
  }

  return null;
}
