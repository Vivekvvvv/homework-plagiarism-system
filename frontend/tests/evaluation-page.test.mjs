import assert from "node:assert/strict";

import { riskText, toPercent } from "../.test-dist/src/views/evaluation.logic.js";

export function runEvaluationPageTests() {
  assert.equal(riskText(1), "低");
  assert.equal(riskText(2), "中");
  assert.equal(riskText(3), "高");
  assert.equal(toPercent(0.8123), "81.23%");
  assert.equal(toPercent(0), "0%");
}
