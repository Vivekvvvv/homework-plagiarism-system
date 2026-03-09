param(
  [Parameter(Mandatory = $true)]
  [string]$InputFile,
  [string]$DbHost = "127.0.0.1",
  [int]$DbPort = 3306,
  [string]$DbUser = "root",
  [string]$DbPassword = "root"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $InputFile)) {
  throw "Input file not found: $InputFile"
}

Get-Content -Path $InputFile -Raw | & mysql --host=$DbHost --port=$DbPort --user=$DbUser --password=$DbPassword

Write-Host "Restore completed from: $InputFile"
