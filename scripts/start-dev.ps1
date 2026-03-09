param(
  [int]$BackendPort = 8081,
  [int]$FrontendPort = 5173
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendJar = Join-Path $root "backend/target/homework-backend-0.1.0.jar"
$frontendDir = Join-Path $root "frontend"
$uploadDir = Join-Path $root "uploads"

if (-not (Test-Path $backendJar)) {
  throw "backend jar not found: $backendJar"
}

New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null

$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3306"
$env:DB_NAME = "homework_plagiarism"
$env:DB_USER = "root"
$env:DB_PASSWORD = "root"
$env:UPLOAD_DIR = $uploadDir

Start-Process -FilePath "java" -ArgumentList @(
  "-jar",
  $backendJar,
  "--server.port=$BackendPort"
) -WorkingDirectory (Split-Path $backendJar -Parent)

Start-Process -FilePath "npm" -ArgumentList @(
  "run",
  "dev",
  "--",
  "--host",
  "127.0.0.1",
  "--port",
  "$FrontendPort"
) -WorkingDirectory $frontendDir

Write-Host "Backend: http://localhost:$BackendPort"
Write-Host "Frontend: http://localhost:$FrontendPort"
