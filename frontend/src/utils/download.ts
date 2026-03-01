export function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  window.URL.revokeObjectURL(url);
}

export function downloadCsv(data: BlobPart, filename: string): void {
  const blob = new Blob([data], { type: "text/csv;charset=utf-8;" });
  downloadBlob(blob, filename);
}
