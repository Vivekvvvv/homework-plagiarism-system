import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { resolveRouteRedirect } from "./access";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: () => import("../views/LoginView.vue") },
    { path: "/", redirect: "/dashboard" },
    { path: "/dashboard", name: "dashboard", component: () => import("../views/DashboardView.vue") },
    { path: "/courses", name: "courses", component: () => import("../views/CoursesView.vue") },
    { path: "/assignments", name: "assignments", component: () => import("../views/AssignmentsView.vue") },
    { path: "/submissions", name: "submissions", component: () => import("../views/SubmissionsView.vue") },
    { path: "/plagiarism", name: "plagiarism", component: () => import("../views/PlagiarismView.vue") },
    { path: "/reviews", name: "reviews", component: () => import("../views/ReviewsView.vue") },
    { path: "/evaluation", name: "evaluation", component: () => import("../views/EvaluationView.vue") },
    { path: "/analytics", name: "analytics", component: () => import("../views/AnalyticsView.vue") },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const initialRedirect = resolveRouteRedirect({
    toPath: to.path,
    token: authStore.token,
    role: authStore.user?.role,
  });
  if (initialRedirect === "/login" || initialRedirect === "/dashboard") {
    return initialRedirect;
  }
  if (authStore.token && !authStore.user) {
    try {
      await authStore.fetchMe();
    } catch {
      authStore.logout();
      return "/login";
    }
  }
  const resolvedRedirect = resolveRouteRedirect({
    toPath: to.path,
    token: authStore.token,
    role: authStore.user?.role,
  });
  if (resolvedRedirect) {
    return resolvedRedirect;
  }
  return true;
});

export default router;
