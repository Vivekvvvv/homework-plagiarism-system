const TEACHER_ROLES = new Set(["ADMIN", "TEACHER"]);
const ADMIN_ONLY_ROLES = new Set(["ADMIN"]);

export const teacherOnlyPaths = [
  "/courses",
  "/assignments",
  "/plagiarism",
  "/reviews",
  "/evaluation",
  "/analytics",
] as const;

const adminOnlyPaths = ["/users"] as const;

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
    adminOnlyPaths.includes(toPath as (typeof adminOnlyPaths)[number]) &&
    normalizedRole &&
    !ADMIN_ONLY_ROLES.has(normalizedRole)
  ) {
    return "/dashboard";
  }

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
