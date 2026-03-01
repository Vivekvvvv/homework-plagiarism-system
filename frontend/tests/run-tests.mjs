import { runApiConfigTests } from "./api-config.test.mjs";
import { runDashboardPageTests } from "./dashboard-page.test.mjs";
import { runEvaluationPageTests } from "./evaluation-page.test.mjs";
import { runLoginPageTests } from "./login-page.test.mjs";
import { runPlagiarismPageTests } from "./plagiarism-page.test.mjs";
import { runReviewsPageTests } from "./reviews-page.test.mjs";
import { runRouterAccessTests } from "./router-access.test.mjs";
import { runRouterQueryTests } from "./router-query.test.mjs";
import { runSubmissionsPageTests } from "./submissions-page.test.mjs";

const testSuites = [
  ["api-config", runApiConfigTests],
  ["dashboard-page", runDashboardPageTests],
  ["evaluation-page", runEvaluationPageTests],
  ["login-page", runLoginPageTests],
  ["plagiarism-page", runPlagiarismPageTests],
  ["reviews-page", runReviewsPageTests],
  ["router-access", runRouterAccessTests],
  ["router-query", runRouterQueryTests],
  ["submissions-page", runSubmissionsPageTests],
];

let failed = false;

for (const [name, run] of testSuites) {
  try {
    run();
    console.log(`PASS ${name}`);
  } catch (error) {
    failed = true;
    console.error(`FAIL ${name}`);
    console.error(error);
  }
}

if (failed) {
  process.exit(1);
}

console.log("All frontend tests passed.");
