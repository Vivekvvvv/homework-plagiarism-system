param(
  [string]$BaseUrl = "http://localhost:8080/api/v1",
  [string]$Username = "admin",
  [string]$Password = "123456",
  [int]$TimeoutSec = 120,
  [string]$OutputDir = "./artifacts/smoke"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Build-QueryString {
  param([hashtable]$Query)

  if ($null -eq $Query -or $Query.Count -eq 0) {
    return ""
  }

  $pairs = foreach ($entry in $Query.GetEnumerator()) {
    $key = [System.Uri]::EscapeDataString([string]$entry.Key)
    $value = [System.Uri]::EscapeDataString([string]$entry.Value)
    "${key}=${value}"
  }
  return ($pairs -join "&")
}

function Invoke-ApiJson {
  param(
    [ValidateSet("GET", "POST", "PATCH", "PUT")]
    [string]$Method,
    [string]$Path,
    [hashtable]$Body,
    [string]$Token,
    [hashtable]$Query
  )

  $uri = "$BaseUrl$Path"
  $queryString = Build-QueryString -Query $Query
  if ($queryString) {
    $uri = "${uri}?$queryString"
  }

  $headers = @{ Accept = "application/json" }
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }

  $requestParams = @{
    Method = $Method
    Uri = $uri
    Headers = $headers
    UseBasicParsing = $true
  }

  if ($null -ne $Body) {
    $payload = $Body | ConvertTo-Json -Depth 8
    $requestParams["ContentType"] = "application/json; charset=utf-8"
    $requestParams["Body"] = $payload
  }

  $response = Invoke-WebRequest @requestParams
  $rawText = $response.Content
  if ($response.RawContentStream) {
    $buffer = New-Object System.IO.MemoryStream
    $response.RawContentStream.CopyTo($buffer)
    $rawText = [System.Text.Encoding]::UTF8.GetString($buffer.ToArray())
    $buffer.Dispose()
  }

  $json = $rawText | ConvertFrom-Json
  if ($json.code -ne 0) {
    throw "API 调用失败：$Method $Path，code=$($json.code)，msg=$($json.msg)"
  }

  return $json.data
}

function Invoke-ApiDownload {
  param(
    [string]$Path,
    [string]$Token,
    [hashtable]$Query,
    [string]$OutFile
  )

  $uri = "$BaseUrl$Path"
  $queryString = Build-QueryString -Query $Query
  if ($queryString) {
    $uri = "${uri}?$queryString"
  }

  $headers = @{}
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }

  Invoke-WebRequest -Method GET -Uri $uri -Headers $headers -OutFile $OutFile | Out-Null
}

function Invoke-FileUpload {
  param(
    [string]$Path,
    [string]$Token,
    [string]$FilePath
  )

  $uri = "$BaseUrl$Path"
  $raw = & curl.exe -s -X POST $uri -H "Authorization: Bearer $Token" -F "file=@$FilePath"
  if ([string]::IsNullOrWhiteSpace($raw)) {
    throw "文件上传失败：curl 未返回内容"
  }

  $json = $raw | ConvertFrom-Json
  if ($json.code -ne 0) {
    throw "文件上传失败：code=$($json.code)，msg=$($json.msg)"
  }

  return $json.data
}

Write-Host "[1/9] 登录并获取 Token..."
$loginData = Invoke-ApiJson -Method POST -Path "/auth/login" -Body @{ username = $Username; password = $Password } -Token $null -Query $null
$token = $loginData.token
if ([string]::IsNullOrWhiteSpace($token)) {
  throw "登录成功但未返回 token"
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$suffix = Get-Date -Format "yyyyMMddHHmmss"

Write-Host "[2/9] 新建课程..."
$course = Invoke-ApiJson -Method POST -Path "/courses" -Body @{
  courseCode = "SE-SMOKE-$suffix"
  courseName = "毕业设计冒烟课程-$suffix"
  teacherId = 2
  semester = "2025-2026-2"
} -Token $token -Query $null
$courseId = [long]$course.id

Write-Host "[3/9] 新建作业..."
$assignment = Invoke-ApiJson -Method POST -Path "/assignments" -Body @{
  courseId = $courseId
  title = "冒烟作业-$suffix"
  description = "自动化冒烟测试作业"
  deadline = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm:ss")
  maxScore = 100
  createdBy = 2
} -Token $token -Query $null
$assignmentId = [long]$assignment.id

Write-Host "[3.5/9] 配置 Rubric..."
$null = Invoke-ApiJson -Method PUT -Path "/reviews/rubric" -Body @{
  assignmentId = $assignmentId
  items = @(
    @{ dimension = "需求完成度"; weight = 40; description = "功能完整性" },
    @{ dimension = "工程质量"; weight = 35; description = "代码质量与可维护性" },
    @{ dimension = "文档表达"; weight = 25; description = "文档清晰性" }
  )
} -Token $token -Query $null

Write-Host "[4/9] 创建文本提交（两份）..."
$submissionA = Invoke-ApiJson -Method POST -Path "/submissions" -Body @{
  assignmentId = $assignmentId
  studentId = 3
  rawText = "本次实验关注系统架构设计、模块拆分与接口约定，强调可维护性与工程实践。"
} -Token $token -Query $null
$submissionB = Invoke-ApiJson -Method POST -Path "/submissions" -Body @{
  assignmentId = $assignmentId
  studentId = 4
  rawText = "本次实验重点是系统架构设计、模块拆分和接口约定，强调维护性与工程实现。"
} -Token $token -Query $null
$submissionAId = [long]$submissionA.id
$submissionBId = [long]$submissionB.id

Write-Host "[5/9] 上传文件并创建文件提交..."
$sampleFile = Join-Path $OutputDir "smoke_submission_$suffix.txt"
Set-Content -Path $sampleFile -Value "这是用于冒烟测试的文件提交内容。" -Encoding UTF8
$uploadData = Invoke-FileUpload -Path "/files/upload" -Token $token -FilePath $sampleFile
$fileSubmission = Invoke-ApiJson -Method POST -Path "/submissions" -Body @{
  assignmentId = $assignmentId
  studentId = 5
  fileId = [long]$uploadData.fileId
} -Token $token -Query $null
$fileSubmissionId = [long]$fileSubmission.id

Write-Host "[6/9] 发起查重任务并轮询完成..."
$task = Invoke-ApiJson -Method POST -Path "/plagiarism/tasks" -Body @{
  assignmentId = $assignmentId
  threshold = 0.70
  simhashWeight = 0.70
  jaccardWeight = 0.30
  maxRetry = 1
  runTimeoutSeconds = 180
  idempotencyKey = "smoke-$suffix"
} -Token $token -Query $null
$taskId = [long]$task.id

$terminalStatus = @(2, 3, 4)
$startAt = Get-Date
$taskStatus = -1
while ($true) {
  Start-Sleep -Seconds 2
  $currentTask = Invoke-ApiJson -Method GET -Path "/plagiarism/tasks/$taskId" -Body $null -Token $token -Query $null
  $taskStatus = [int]$currentTask.status
  Write-Host "  - task=$taskId status=$taskStatus"

  if ($terminalStatus -contains $taskStatus) {
    break
  }

  if (((Get-Date) - $startAt).TotalSeconds -ge $TimeoutSec) {
    throw "查重任务超时（>${TimeoutSec}s），taskId=$taskId"
  }
}

if ($taskStatus -ne 2) {
  throw "查重任务未成功结束，最终状态=$taskStatus"
}

$report = Invoke-ApiJson -Method GET -Path "/plagiarism/tasks/$taskId/report" -Body $null -Token $token -Query $null
$pairPage = Invoke-ApiJson -Method GET -Path "/plagiarism/tasks/$taskId/pairs" -Body $null -Token $token -Query @{ pageNo = 1; pageSize = 20 }
$pairList = @($pairPage.records)
$pairCount = $pairList.Count
$pairId = $null
if ($pairCount -gt 0) {
  $pairId = [long]$pairList[0].id
  $null = Invoke-ApiJson -Method GET -Path "/plagiarism/pairs/$pairId" -Body $null -Token $token -Query $null
}

$pairsCsv = Join-Path $OutputDir "plagiarism_pairs_$taskId.csv"
$assignmentCsv = Join-Path $OutputDir "assignment_${assignmentId}_report.csv"
Invoke-ApiDownload -Path "/plagiarism/tasks/$taskId/pairs/export" -Token $token -Query $null -OutFile $pairsCsv
Invoke-ApiDownload -Path "/plagiarism/assignments/$assignmentId/report/export" -Token $token -Query $null -OutFile $assignmentCsv

Write-Host "[7/9] 写入评阅并查询汇总..."
$null = Invoke-ApiJson -Method POST -Path "/reviews" -Body @{
  submissionId = $submissionAId
  score = 88.5
  comment = "结构清晰，论证充分，建议补充实验对照数据。"
} -Token $token -Query $null

$reviewSummary = Invoke-ApiJson -Method GET -Path "/reviews/summary" -Body $null -Token $token -Query @{ assignmentId = $assignmentId }
$reviewRows = Invoke-ApiJson -Method GET -Path "/reviews" -Body $null -Token $token -Query @{ assignmentId = $assignmentId }
$reviewSuggestion = Invoke-ApiJson -Method GET -Path "/reviews/suggestion" -Body $null -Token $token -Query @{ score = 88.5 }
$evolutionRows = Invoke-ApiJson -Method GET -Path "/submissions/evolution" -Body $null -Token $token -Query @{ assignmentId = $assignmentId; studentId = 3 }
$reviewCsv = Join-Path $OutputDir "assignment_${assignmentId}_reviews.csv"
Invoke-ApiDownload -Path "/reviews/export" -Token $token -Query @{ assignmentId = $assignmentId } -OutFile $reviewCsv

Write-Host "[8/9] 验证课程与作业查询接口..."
$courses = Invoke-ApiJson -Method GET -Path "/courses" -Body $null -Token $token -Query $null
$assignments = Invoke-ApiJson -Method GET -Path "/assignments" -Body $null -Token $token -Query @{ courseId = $courseId }

Write-Host "[9/9] 冒烟测试完成，输出结果："
$rawReviewedRate = $reviewSummary.reviewedRate
$normalizedReviewedRate = $rawReviewedRate
if ($null -ne $rawReviewedRate) {
  $rateValue = [double]$rawReviewedRate
  if ($rateValue -gt 1 -and $rateValue -le 100) {
    $normalizedReviewedRate = [Math]::Round(($rateValue / 100.0), 4)
  }
}
$result = [ordered]@{
  baseUrl = $BaseUrl
  courseId = $courseId
  assignmentId = $assignmentId
  submissionIds = @($submissionAId, $submissionBId, $fileSubmissionId)
  plagiarismTaskId = $taskId
  plagiarismPairCount = $pairCount
  reviewCount = @($reviewRows).Count
  reviewedRate = $normalizedReviewedRate
  reviewSuggestion = $reviewSuggestion.suggestion
  evolutionPointCount = @($evolutionRows).Count
  exportedFiles = @($pairsCsv, $assignmentCsv, $reviewCsv)
  coursesTotal = @($courses).Count
  assignmentsInCourse = @($assignments).Count
}

$resultJson = $result | ConvertTo-Json -Depth 6
$resultFile = Join-Path $OutputDir "smoke_result_$suffix.json"
Set-Content -Path $resultFile -Value $resultJson -Encoding UTF8
Write-Host $resultJson
Write-Host "结果文件：$resultFile"
