import assert from "node:assert/strict";

import {
  buildFeedbackSummary,
  buildSubmissionDisplayRows,
  resolveLatestVersionNo,
  resolveReviewStatusTagType,
  validateSubmissionInput,
} from "../.test-dist/src/views/submissions.logic.js";

export function runSubmissionsPageTests() {
  const submissions = [
    {
      id: 11,
      studentId: 3,
      versionNo: 2,
      sourceType: 1,
      tokenCount: 120,
      submitTime: "2026-03-10T10:00:00",
      contentHash: "hash-a",
    },
    {
      id: 9,
      studentId: 3,
      versionNo: 1,
      sourceType: 2,
      tokenCount: 80,
      submitTime: "2026-03-09T09:00:00",
      contentHash: "hash-b",
    },
  ];
  const reviews = [
    {
      submissionId: 11,
      assignmentId: 7,
      studentId: 3,
      score: 88.5,
      comment: "good",
      reviewedAt: "2026-03-10T11:00:00",
    },
  ];

  const displayRows = buildSubmissionDisplayRows(submissions, reviews);
  assert.equal(displayRows.length, 2);
  assert.equal(displayRows[0].reviewStatus, "reviewed");
  assert.equal(displayRows[1].reviewStatus, "pending");
  assert.equal(resolveReviewStatusTagType("reviewed"), "success");
  assert.equal(resolveReviewStatusTagType("pending"), "warning");

  const summary = buildFeedbackSummary(displayRows);
  assert.equal(summary.total, 2);
  assert.equal(summary.reviewed, 1);
  assert.equal(summary.pending, 1);
  assert.equal(summary.latest?.id, 11);

  assert.equal(resolveLatestVersionNo(displayRows), 2);

  assert.equal(
    validateSubmissionInput({
      mode: "file",
      assignmentId: 1,
      studentId: 2,
      file: null,
    }),
    "请先选择文件"
  );
  assert.equal(
    validateSubmissionInput({
      mode: "file",
      assignmentId: 1,
      studentId: 2,
      file: { size: 11 * 1024 * 1024 },
    }),
    "上传文件大小不能超过 10MB"
  );
  assert.equal(
    validateSubmissionInput({
      mode: "text",
      assignmentId: 1,
      studentId: 2,
      rawText: "   ",
    }),
    "请输入作业文本"
  );
  assert.equal(
    validateSubmissionInput({
      mode: "text",
      assignmentId: 1,
      studentId: 2,
      rawText: "valid",
    }),
    null
  );
}
