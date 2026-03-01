import assert from "node:assert/strict";

import {
  buildTrendChartData,
  phaseTagType,
  phaseText,
  riskTagType,
  riskText,
  statusTagType,
  statusText,
  trendRiskPercent,
} from "../.test-dist/src/views/plagiarism.logic.js";

export function runPlagiarismPageTests() {
  assert.equal(statusText(0), "Pending");
  assert.equal(statusText(1), "Running");
  assert.equal(statusTagType(2), "success");
  assert.equal(statusTagType(3), "danger");

  assert.equal(riskText(1), "Low");
  assert.equal(riskText(2), "Medium");
  assert.equal(riskText(3), "High");
  assert.equal(riskTagType(2), "warning");

  assert.equal(phaseText("CREATED"), "Created");
  assert.equal(phaseText("RETRY_SCHEDULED"), "Retry Scheduled");
  assert.equal(phaseTagType("FAILED"), "danger");

  assert.equal(trendRiskPercent(0.345), 34.5);

  const chart = buildTrendChartData(
    [
      { taskId: 1, highRiskRate: 0.1 },
      { taskId: 2, highRiskRate: 0.25 },
    ],
    760,
    220,
    { left: 50, right: 20, top: 16, bottom: 30 }
  );
  assert.equal(chart.points.length, 2);
  assert.equal(chart.yTicks.length, 5);
  assert.ok(chart.polyline.includes(","));
}
