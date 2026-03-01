export function riskText(risk: number): string {
  if (risk === 3) return "高";
  if (risk === 2) return "中";
  return "低";
}

export function toPercent(value: number): string {
  return `${Number((Number(value || 0) * 100).toFixed(2))}%`;
}

export function toPercentNumber(value: number): number {
  return Number((Number(value || 0) * 100).toFixed(2));
}

export function formatPercent(value: number): string {
  return `${Number(value || 0).toFixed(2)}%`;
}

export type EvalRunPoint = {
  x: number;
  y: number;
  label: string;
  value: number;
  showLabel: boolean;
};

export type EvalRunChartData = {
  yTicks: Array<{ value: number; y: number }>;
  accuracyPoints: EvalRunPoint[];
  recallPoints: EvalRunPoint[];
  f1Points: EvalRunPoint[];
  accuracyPolyline: string;
  recallPolyline: string;
  f1Polyline: string;
};

export function buildEvalRunChartData(
  rows: Array<{ id: number; accuracy: number; macroRecall: number; macroF1: number }>,
  width: number,
  height: number,
  padding: { left: number; right: number; top: number; bottom: number }
): EvalRunChartData {
  const maxValue = 100;
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;
  const step = Math.max(1, Math.ceil(rows.length / 8));

  const yTicks = [0, 25, 50, 75, 100].map((value) => ({
    value,
    y: padding.top + ((maxValue - value) / maxValue) * plotHeight,
  }));

  const pointsFor = (key: "accuracy" | "macroRecall" | "macroF1") =>
    rows.map((row, idx) => {
      const raw = Number(row[key] || 0) * 100;
      const value = Number(raw.toFixed(2));
      const x = rows.length === 1 ? padding.left + plotWidth / 2 : padding.left + (idx * plotWidth) / (rows.length - 1);
      const y = padding.top + ((maxValue - value) / maxValue) * plotHeight;
      return {
        x,
        y,
        label: `R${row.id}`,
        value,
        showLabel: idx % step === 0 || idx === rows.length - 1,
      };
    });

  const accuracyPoints = pointsFor("accuracy");
  const recallPoints = pointsFor("macroRecall");
  const f1Points = pointsFor("macroF1");

  return {
    yTicks,
    accuracyPoints,
    recallPoints,
    f1Points,
    accuracyPolyline: accuracyPoints.map((point) => `${point.x},${point.y}`).join(" "),
    recallPolyline: recallPoints.map((point) => `${point.x},${point.y}`).join(" "),
    f1Polyline: f1Points.map((point) => `${point.x},${point.y}`).join(" "),
  };
}
