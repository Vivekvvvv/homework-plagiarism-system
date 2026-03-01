# Homework Backend

## Quick Start

1. Execute `src/main/resources/sql/init.sql` in MySQL.
2. Configure env vars if needed:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`
3. Start backend:

```bash
mvn spring-boot:run
```

## Database Compatibility

- `init.sql` uses `utf8mb4_0900_ai_ci`, which requires MySQL 8.0+.
- If you are on MySQL 5.7 or MariaDB, replace the collation with
  `utf8mb4_unicode_ci` (or `utf8mb4_general_ci`) before executing.
- Recommended charset: `utf8mb4`.

## Test

```bash
mvn test
```

## Ops

- Health: `GET /actuator/health`
- Info: `GET /actuator/info`

## Default Account

- Password (all): `root`
- `admin` (`ADMIN`)
- `teacher1` (`TEACHER`)
- `student1` (`STUDENT`)
- `student2` (`STUDENT`)
- `student3` (`STUDENT`)

## Role Matrix

| Module | STUDENT | TEACHER | ADMIN |
| --- | --- | --- | --- |
| Course create | ❌ | ✅ (own courses) | ✅ |
| Assignment create | ❌ | ✅ (own courses) | ✅ |
| Submission create | ✅ (self studentId) | ✅ | ✅ |
| Submission list/text | ✅ (self only) | ✅ (own courses) | ✅ |
| Plagiarism task/report/trend | ❌ | ✅ (own courses) | ✅ |
| Review/rubric | ✅ (own feedback only) | ✅ (own courses) | ✅ |
| Evaluation module | ❌ | ✅ | ✅ |
| Audit log query | ❌ | ✅ | ✅ |
| File download | ✅ (self uploaded) | ✅ | ✅ |

## API

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- Base path: `/api/v1`

## Submission Module Endpoints

- `POST /api/v1/files/upload` (multipart file upload)
- `GET /api/v1/files/{id}/download` (student can download own files only)
- `POST /api/v1/submissions` (create submission by fileId or rawText)
- `GET /api/v1/submissions?assignmentId=1`
- `GET /api/v1/submissions/{submissionId}/text` (student can access own submission only)

## Plagiarism Module Endpoints

- `POST /api/v1/plagiarism/tasks`
- `GET /api/v1/plagiarism/tasks?assignmentId=1`
- `GET /api/v1/plagiarism/tasks/latest?assignmentId=1`
- `GET /api/v1/plagiarism/tasks/{taskId}`
- `GET /api/v1/plagiarism/tasks/{taskId}/report`
- `PATCH /api/v1/plagiarism/tasks/{taskId}/cancel`
- `POST /api/v1/plagiarism/tasks/{taskId}/retry`
- `GET /api/v1/plagiarism/tasks/{taskId}/logs`
- `GET /api/v1/plagiarism/tasks/{taskId}/pairs?riskLevel=3&minSimilarity=0.8&pageNo=1&pageSize=20`
- `GET /api/v1/plagiarism/tasks/{taskId}/pairs/export?riskLevel=3&minSimilarity=0.8`
- `GET /api/v1/plagiarism/assignments/{assignmentId}/report/export`
- `GET /api/v1/plagiarism/assignments/{assignmentId}/trend?startAt=2026-02-01T00:00:00&endAt=2026-02-12T23:59:59&limit=50`
- `GET /api/v1/plagiarism/pairs/{pairId}`

Create task request now supports:

- `simhashWeight` / `jaccardWeight`
- `idempotencyKey`
- `maxRetry`
- `runTimeoutSeconds`

## Review Module Endpoints

- `POST /api/v1/reviews`
- `GET /api/v1/reviews?assignmentId=1` (student can read own feedback rows only)
- `GET /api/v1/reviews/summary?assignmentId=1` (student summary is scoped to own submissions)
- `GET /api/v1/reviews/submissions/{submissionId}` (student can read own feedback only)
- `GET /api/v1/reviews/export?assignmentId=1`
- `PUT /api/v1/reviews/rubric`
- `GET /api/v1/reviews/rubric?assignmentId=1`
- `GET /api/v1/reviews/suggestion?score=88`

## Submission Evolution Endpoint

- `GET /api/v1/submissions/evolution?assignmentId=1&studentId=3`

## Evaluation Module Endpoints

- `POST /api/v1/plagiarism/evaluation/cases`
- `GET /api/v1/plagiarism/evaluation/cases?enabled=1`
- `POST /api/v1/plagiarism/evaluation/run?threshold=0.7&simhashWeight=0.7&jaccardWeight=0.3`
- `GET /api/v1/plagiarism/evaluation/report`

## Audit Module Endpoints

- `GET /api/v1/audit/logs?actorUsername=admin&action=PLAGIARISM_TASK_CREATE&limit=100`

## Notes

- Re-run `src/main/resources/sql/init.sql` to ensure the latest schema is applied
  (including role field, audit/evaluation tables, plagiarism task extensions,
  and review/rubric related schema).
- Default datasource env values:
  - `DB_HOST=127.0.0.1`
  - `DB_PORT=3306`
  - `DB_NAME=homework_plagiarism`
  - `DB_USER=root`
  - `DB_PASSWORD=root`
- Task execution is asynchronous:
  - `0` pending
  - `1` running
  - `2` success
  - `3` failed
  - `4` canceled
- Failed task message can be read from `errorMessage` in task payload.
- End-to-end smoke test script is available at repository root:
  `../scripts/smoke-test.ps1`.
- Demo bootstrap script is available at repository root:
  `../scripts/demo-bootstrap.ps1`.
- GitHub Actions CI runs backend tests plus frontend test/build verification.
- Production deployment guide: `../PRODUCTION_DEPLOYMENT.md`.
