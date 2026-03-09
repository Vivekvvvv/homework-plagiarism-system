param(
  [string]$BaseUrl = "http://localhost:8080/api/v1",
  [string]$Username = "admin",
  [string]$Password = "123456",
  [int]$TimeoutSec = 120,
  [string]$OutputDir = "./artifacts/smoke"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-FileExists {
  param([string]$Path)
  if (-not (Test-Path -Path $Path)) {
    throw "回归检查失败：找不到文件 $Path"
  }
  $item = Get-Item -Path $Path
  if ($item.Length -le 0) {
    throw "回归检查失败：文件为空 $Path"
  }
}

Write-Host "[1/3] 执行冒烟脚本..."
$smokeScript = Join-Path $PSScriptRoot "smoke-test.ps1"
& $smokeScript -BaseUrl $BaseUrl -Username $Username -Password $Password -TimeoutSec $TimeoutSec -OutputDir $OutputDir

Write-Host "[2/3] 读取最新回归结果..."
$latest = Get-ChildItem -Path $OutputDir -Filter "smoke_result_*.json" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $latest) {
  throw "回归检查失败：未找到 smoke_result_*.json"
}

$result = Get-Content -Path $latest.FullName -Encoding UTF8 | ConvertFrom-Json
if (-not $result.plagiarismTaskId) {
  throw "回归检查失败：plagiarismTaskId 为空"
}
if ($result.reviewedRate -lt 0 -or $result.reviewedRate -gt 1) {
  throw "回归检查失败：reviewedRate 超出范围"
}

Write-Host "[3/3] 校验导出文件..."
foreach ($file in $result.exportedFiles) {
  Assert-FileExists -Path $file
}

Write-Host "部署后回归检查通过。结果文件：$($latest.FullName)"
