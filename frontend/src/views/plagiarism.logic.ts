export type TrendRow = {
  taskId: number;
  highRiskRate: number;
};

type TrendChartPadding = {
  left: number;
  right: number;
  top: number;
  bottom: number;
};

type TrendChartPoint = {
  x: number;
  y: number;
  label: string;
  rate: number;
  showLabel: boolean;
};

type TrendChartData = {
  maxRate: number;
  points: TrendChartPoint[];
  yTicks: Array<{ value: number; y: number }>;
  polyline: string;
};

export function statusText(status: number): string {
  if (status === 2) return "成功";
  if (status === 1) return "运行中";
  if (status === 3) return "失败";
  if (status === 4) return "已取消";
  return "等待中";
}

export function statusTagType(status: number): "success" | "warning" | "danger" | "info" {
  if (status === 2) return "success";
  if (status === 1) return "warning";
  if (status === 3) return "danger";
  return "info";
}

export function riskText(riskLevel: number): string {
  if (riskLevel === 3) return "高风险";
  if (riskLevel === 2) return "中风险";
  return "低风险";
}

export function riskTagType(riskLevel: number): "danger" | "warning" | "success" {
  if (riskLevel === 3) return "danger";
  if (riskLevel === 2) return "warning";
  return "success";
}

export function phaseText(phase: string): string {
  if (phase === "CREATED") return "已创建";
  if (phase === "STARTED") return "已启动";
  if (phase === "RUNNING") return "运行中";
  if (phase === "SUCCEEDED") return "已成功";
  if (phase === "FAILED") return "已失败";
  if (phase === "CANCELED") return "已取消";
  if (phase === "RETRY_SCHEDULED") return "等待重试";
  return phase;
}

export function phaseTagType(phase: string): "success" | "danger" | "info" | "warning" | "primary" {
  if (phase === "SUCCEEDED") return "success";
  if (phase === "FAILED") return "danger";
  if (phase === "CANCELED") return "info";
  if (phase === "RETRY_SCHEDULED") return "warning";
  if (phase === "RUNNING" || phase === "STARTED") return "warning";
  return "primary";
}

export function trendRiskPercent(rate: number): number {
  return Number(((rate || 0) * 100).toFixed(1));
}

export function buildTrendChartData(
  rows: TrendRow[],
  chartWidth: number,
  chartHeight: number,
  padding: TrendChartPadding
): TrendChartData {
  const plotWidth = chartWidth - padding.left - padding.right;
  const plotHeight = chartHeight - padding.top - padding.bottom;
  const rates = rows.map((row) => trendRiskPercent(Number(row.highRiskRate || 0)));
  const maxRate = rates.length === 0 ? 100 : Math.max(10, Math.ceil(Math.max(...rates) / 10) * 10);
  const labelStep = Math.max(1, Math.ceil(rows.length / 8));

  const points: TrendChartPoint[] = rows.map((row, idx) => {
    const rate = trendRiskPercent(Number(row.highRiskRate || 0));
    const x =
      rows.length === 1 ? padding.left + plotWidth / 2 : padding.left + (idx * plotWidth) / (rows.length - 1);
    const y = padding.top + ((maxRate - rate) / maxRate) * plotHeight;
    return {
      x,
      y,
      label: `T${row.taskId}`,
      rate,
      showLabel: idx % labelStep === 0 || idx === rows.length - 1,
    };
  });

  const yTicks = Array.from({ length: 5 }, (_, i) => {
    const value = Number((maxRate - (maxRate * i) / 4).toFixed(0));
    const y = padding.top + (plotHeight * i) / 4;
    return { value, y };
  });

  return {
    maxRate,
    points,
    yTicks,
    polyline: points.map((point) => `${point.x},${point.y}`).join(" "),
  };
}
