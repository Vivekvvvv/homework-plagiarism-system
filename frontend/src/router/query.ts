export function readPositiveIntQuery(value: unknown, fallback: number): number {
  const first = Array.isArray(value) ? value[0] : value;
  const parsed = Number(first);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return fallback;
  }
  return parsed;
}

export function readOptionalPositiveIntQuery(value: unknown): number | null {
  const first = Array.isArray(value) ? value[0] : value;
  if (first === null || first === undefined || first === "") {
    return null;
  }
  const parsed = Number(first);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return null;
  }
  return parsed;
}

export function readStringQuery(value: unknown): string | null {
  const first = Array.isArray(value) ? value[0] : value;
  if (first === null || first === undefined) {
    return null;
  }
  const normalized = String(first).trim();
  return normalized ? normalized : null;
}
