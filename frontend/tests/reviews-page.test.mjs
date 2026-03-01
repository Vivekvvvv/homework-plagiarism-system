import assert from "node:assert/strict";

import {
  buildDimensionScores,
  filterReviewRows,
  resolveWeightedScore,
} from "../.test-dist/src/views/reviews.logic.js";

export function runReviewsPageTests() {
  const rows = [
    { submissionId: 1, score: null },
    { submissionId: 2, score: 88 },
  ];
  assert.equal(filterReviewRows(rows, false).length, 2);
  assert.equal(filterReviewRows(rows, true).length, 1);

  const rubricItems = [
    { dimension: "完整性", weight: 40 },
    { dimension: "质量", weight: 60 },
  ];
  const scores = buildDimensionScores(rubricItems, JSON.stringify([{ dimension: "质量", score: 95, comment: "good" }]));
  assert.equal(scores.length, 2);
  assert.equal(scores[0].score, 60);
  assert.equal(scores[1].score, 95);

  const weighted = resolveWeightedScore([
    { dimension: "A", weight: 40, score: 80 },
    { dimension: "B", weight: 60, score: 100 },
  ]);
  assert.equal(weighted, 92);
}
