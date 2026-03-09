param(
  [string]$DbHost = "127.0.0.1",
  [int]$DbPort = 3306,
  [string]$DbName = "homework_plagiarism",
  [string]$DbUser = "root",
  [string]$DbPassword = "root",
  [string]$OutputDir = "./artifacts/backup",
  [int]$RetentionDays = 7,
  [int]$MaxBackups = 20
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$backupFile = Join-Path $OutputDir "${DbName}_${timestamp}.sql"

& mysqldump --host=$DbHost --port=$DbPort --user=$DbUser --password=$DbPassword --databases $DbName `
  | Out-File -FilePath $backupFile -Encoding utf8

if ($RetentionDays -gt 0) {
  Get-ChildItem -Path $OutputDir -Filter "${DbName}_*.sql" -File |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    Remove-Item -Force
}

if ($MaxBackups -gt 0) {
  $backups = Get-ChildItem -Path $OutputDir -Filter "${DbName}_*.sql" -File |
    Sort-Object LastWriteTime -Descending
  if ($backups.Count -gt $MaxBackups) {
    $backups | Select-Object -Skip $MaxBackups | Remove-Item -Force
  }
}

Write-Host "Backup file: $backupFile"
