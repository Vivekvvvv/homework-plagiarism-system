import assert from "node:assert/strict";

import { normalizeRole, resolveRouteRedirect, teacherOnlyPaths } from "../.test-dist/src/router/access.js";

export function runRouterAccessTests() {
  assert.deepEqual(teacherOnlyPaths, [
    "/courses",
    "/assignments",
    "/plagiarism",
    "/reviews",
    "/evaluation",
    "/analytics",
  ]);

  assert.equal(normalizeRole(" teacher "), "TEACHER");
  assert.equal(normalizeRole(undefined), "");

  assert.equal(resolveRouteRedirect({ toPath: "/dashboard", token: "" }), "/login");

  assert.equal(
    resolveRouteRedirect({ toPath: "/plagiarism", token: "jwt-token", role: "student" }),
    "/submissions"
  );

  assert.equal(
    resolveRouteRedirect({ toPath: "/plagiarism", token: "jwt-token", role: "teacher" }),
    null
  );

  assert.equal(resolveRouteRedirect({ toPath: "/login", token: "jwt-token" }), "/dashboard");
}
