param(
  [string]$BaseUrl = "http://localhost:8081",
  [string]$Path = "/actuator/health",
  [int]$Requests = 30,
  [int]$TimeoutSec = 10,
  [string]$OutputDir = "./artifacts/perf"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-Percentile {
  param(
    [double[]]$Values,
    [double]$Percentile
  )

  if ($Values.Count -eq 0) {
    return 0
  }

  $sorted = $Values | Sort-Object
  $index = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
  if ($index -lt 0) { $index = 0 }
  return [Math]::Round($sorted[$index], 2)
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

$uri = "$BaseUrl$Path"
$durations = @()
$success = 0
$failed = 0

for ($i = 1; $i -le $Requests; $i++) {
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec $TimeoutSec
    $sw.Stop()
    $durations += [double]$sw.Elapsed.TotalMilliseconds
    if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
      $success++
    } else {
      $failed++
    }
  } catch {
    $sw.Stop()
    $durations += [double]$sw.Elapsed.TotalMilliseconds
    $failed++
  }
}

$avg = if ($durations.Count -gt 0) { [Math]::Round((($durations | Measure-Object -Average).Average), 2) } else { 0 }
$max = if ($durations.Count -gt 0) { [Math]::Round((($durations | Measure-Object -Maximum).Maximum), 2) } else { 0 }
$min = if ($durations.Count -gt 0) { [Math]::Round((($durations | Measure-Object -Minimum).Minimum), 2) } else { 0 }
$p95 = Get-Percentile -Values $durations -Percentile 95
$errorRate = if ($Requests -gt 0) { [Math]::Round(($failed / $Requests), 4) } else { 0 }

$result = [ordered]@{
  generatedAt = (Get-Date).ToString("s")
  baseUrl = $BaseUrl
  path = $Path
  requests = $Requests
  success = $success
  failed = $failed
  errorRate = $errorRate
  minMs = $min
  avgMs = $avg
  p95Ms = $p95
  maxMs = $max
}

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$outputFile = Join-Path $OutputDir "perf_result_${timestamp}.json"
($result | ConvertTo-Json -Depth 4) | Set-Content -Path $outputFile -Encoding UTF8

Write-Host ($result | ConvertTo-Json -Depth 4)
Write-Host "Perf result file: $outputFile"
