export type ReviewRow = {
  submissionId: number;
  assignmentId: number;
  studentId: number;
  versionNo: number;
  sourceType: number;
  tokenCount: number;
  submitTime: string;
  reviewId?: number;
  reviewerId?: number;
  score?: number;
  comment?: string;
  autoComment?: string;
  dimensionScoresJson?: string;
  reviewedAt?: string;
};

export type RubricItem = {
  dimension: string;
  weight: number;
  description?: string;
};

export type DimensionScore = {
  dimension: string;
  weight: number;
  score: number;
  comment?: string;
};

export function filterReviewRows(rows: ReviewRow[], pendingOnly: boolean): ReviewRow[] {
  if (!pendingOnly) {
    return rows;
  }
  return rows.filter((row) => row.score === null || row.score === undefined);
}

export function buildDimensionScores(
  rubricItems: RubricItem[],
  dimensionScoresJson?: string | null,
  defaultScore = 60
): DimensionScore[] {
  let parsed: Array<{ dimension: string; score: number; comment?: string }> = [];

  if (dimensionScoresJson) {
    try {
      parsed = JSON.parse(dimensionScoresJson) as Array<{ dimension: string; score: number; comment?: string }>;
    } catch {
      parsed = [];
    }
  }

  return rubricItems.map((item) => {
    const existed = parsed.find((entry) => entry.dimension === item.dimension);
    return {
      dimension: item.dimension,
      weight: item.weight,
      score: Number(existed?.score ?? defaultScore),
      comment: existed?.comment || "",
    };
  });
}

export function resolveWeightedScore(scores: DimensionScore[]): number | null {
  let weighted = 0;
  let totalWeight = 0;
  for (const item of scores) {
    const weight = Number(item.weight || 0);
    if (weight > 0) {
      weighted += Number(item.score || 0) * weight;
      totalWeight += weight;
    }
  }
  if (totalWeight <= 0) {
    return null;
  }
  return Number((weighted / totalWeight).toFixed(2));
}
