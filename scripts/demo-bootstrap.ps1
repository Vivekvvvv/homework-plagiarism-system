param(
  [string]$BaseUrl = "http://localhost:8080/api/v1",
  [string]$Username = "admin",
  [string]$Password = "123456",
  [string]$OutputDir = "./artifacts/demo"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "[1/3] Run smoke flow..."
powershell -ExecutionPolicy Bypass -File "./scripts/smoke-test.ps1" `
  -BaseUrl $BaseUrl `
  -Username $Username `
  -Password $Password `
  -OutputDir $OutputDir

Write-Host "[2/3] Trigger plagiarism evaluation..."
$loginRes = Invoke-RestMethod -Method POST -Uri "$BaseUrl/auth/login" -ContentType "application/json; charset=utf-8" `
  -Body (@{ username = $Username; password = $Password } | ConvertTo-Json)
if ($loginRes.code -ne 0) {
  throw "Login failed: $($loginRes.msg)"
}
$token = $loginRes.data.token
$headers = @{ Authorization = "Bearer $token" }

$null = Invoke-RestMethod -Method POST -Uri "$BaseUrl/plagiarism/evaluation/run?threshold=0.7&simhashWeight=0.7&jaccardWeight=0.3" -Headers $headers
$reportRes = Invoke-RestMethod -Method GET -Uri "$BaseUrl/plagiarism/evaluation/report" -Headers $headers
if ($reportRes.code -ne 0) {
  throw "Evaluation report failed: $($reportRes.msg)"
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$reportFile = Join-Path $OutputDir ("evaluation_report_" + (Get-Date -Format "yyyyMMddHHmmss") + ".json")
($reportRes.data | ConvertTo-Json -Depth 8) | Set-Content -Path $reportFile -Encoding UTF8

Write-Host "[3/3] Demo bootstrap completed"
Write-Host "Evaluation report file: $reportFile"

