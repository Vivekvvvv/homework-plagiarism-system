export type SubmissionRow = {
  id: number;
  studentId: number;
  versionNo: number;
  sourceType: number;
  tokenCount: number;
  submitTime: string;
  contentHash: string;
};

export type ReviewRow = {
  submissionId: number;
  assignmentId: number;
  studentId: number;
  score?: number | null;
  comment?: string;
  autoComment?: string;
  reviewedAt?: string;
};

export type SubmissionDisplayRow = SubmissionRow & {
  score: number | null;
  comment: string;
  autoComment: string;
  reviewedAt: string;
  reviewStatus: "reviewed" | "pending";
};

export type EvolutionChartPoint = {
  x: number;
  y: number;
  label: string;
  value: number;
  showLabel: boolean;
};

export type EvolutionChartData = {
  yTicks: Array<{ value: number; y: number }>;
  scorePoints: EvolutionChartPoint[];
  changePoints: EvolutionChartPoint[];
  scorePolyline: string;
  changePolyline: string;
};

type FeedbackSummary = {
  total: number;
  reviewed: number;
  pending: number;
  latest: SubmissionDisplayRow | null;
};

const DEFAULT_MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

export function buildSubmissionDisplayRows(rows: SubmissionRow[], reviews: ReviewRow[]): SubmissionDisplayRow[] {
  const reviewMap = new Map(reviews.map((row) => [row.submissionId, row]));
  return rows.map((row) => {
    const review = reviewMap.get(row.id);
    const score = review?.score === null || review?.score === undefined ? null : Number(review.score);
    return {
      ...row,
      score,
      comment: review?.comment || "",
      autoComment: review?.autoComment || "",
      reviewedAt: review?.reviewedAt || "",
      reviewStatus: score === null ? "pending" : "reviewed",
    };
  });
}

export function resolveLatestVersionNo(rows: SubmissionDisplayRow[]): number | null {
  if (rows.length === 0) {
    return null;
  }
  return rows[0].versionNo;
}

export function buildFeedbackSummary(rows: SubmissionDisplayRow[]): FeedbackSummary {
  const reviewedRows = [...rows]
    .filter((row) => row.score !== null)
    .sort((left, right) => {
      const reviewedAtDiff = new Date(right.reviewedAt || 0).getTime() - new Date(left.reviewedAt || 0).getTime();
      if (reviewedAtDiff !== 0) {
        return reviewedAtDiff;
      }
      return right.id - left.id;
    });

  return {
    total: rows.length,
    reviewed: reviewedRows.length,
    pending: Math.max(0, rows.length - reviewedRows.length),
    latest: reviewedRows[0] || null,
  };
}

export function resolveReviewStatusTagType(status: "reviewed" | "pending"): "success" | "warning" {
  return status === "reviewed" ? "success" : "warning";
}

export function validateSubmissionInput(params: {
  mode: "file" | "text";
  file?: { size: number } | null;
  rawText?: string;
  assignmentId?: number | null;
  studentId?: number | null;
  maxFileSizeBytes?: number;
}): string | null {
  if (!params.assignmentId || params.assignmentId <= 0) {
    return "作业ID必须为正数";
  }
  if (!params.studentId || params.studentId <= 0) {
    return "学生ID必须为正数";
  }

  if (params.mode === "file") {
    if (!params.file) {
      return "请先选择文件";
    }
    const maxBytes = params.maxFileSizeBytes ?? DEFAULT_MAX_UPLOAD_BYTES;
    if (params.file.size > maxBytes) {
      const maxMb = Math.max(1, Math.round(maxBytes / (1024 * 1024)));
      return `上传文件大小不能超过 ${maxMb}MB`;
    }
    return null;
  }

  if (!params.rawText || !params.rawText.trim()) {
    return "请输入作业文本";
  }
  return null;
}

export function buildEvolutionChartData(
  rows: Array<{ versionNo: number; score?: number | null; changeRate?: number }>,
  width: number,
  height: number,
  padding: { left: number; right: number; top: number; bottom: number }
): EvolutionChartData {
  const maxValue = 100;
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;
  const step = Math.max(1, Math.ceil(rows.length / 8));

  const yTicks = [0, 25, 50, 75, 100].map((value) => ({
    value,
    y: padding.top + ((maxValue - value) / maxValue) * plotHeight,
  }));

  const scorePoints = rows.map((row, idx) => {
    const score = row.score === null || row.score === undefined ? 0 : Number(row.score);
    const x = rows.length === 1 ? padding.left + plotWidth / 2 : padding.left + (idx * plotWidth) / (rows.length - 1);
    const y = padding.top + ((maxValue - score) / maxValue) * plotHeight;
    return {
      x,
      y,
      label: `v${row.versionNo}`,
      value: Number(score.toFixed(2)),
      showLabel: idx % step === 0 || idx === rows.length - 1,
    };
  });

  const changePoints = rows.map((row, idx) => {
    const rate = Number(((row.changeRate ?? 0) * 100).toFixed(2));
    const x = rows.length === 1 ? padding.left + plotWidth / 2 : padding.left + (idx * plotWidth) / (rows.length - 1);
    const y = padding.top + ((maxValue - rate) / maxValue) * plotHeight;
    return {
      x,
      y,
      label: `v${row.versionNo}`,
      value: rate,
      showLabel: idx % step === 0 || idx === rows.length - 1,
    };
  });

  const scorePolyline = scorePoints.map((point) => `${point.x},${point.y}`).join(" ");
  const changePolyline = changePoints.map((point) => `${point.x},${point.y}`).join(" ");

  return {
    yTicks,
    scorePoints,
    changePoints,
    scorePolyline,
    changePolyline,
  };
}
