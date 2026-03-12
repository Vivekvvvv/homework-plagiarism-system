import json
import subprocess
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:8081/api/v1"
FRONTEND_URL = "http://localhost:5173"
ARTIFACT_DIR = Path("artifacts") / "screenshots" / "p2"


def api_request(method, path, token=None, data=None, params=None):
    url = f"{BASE_URL}{path}"
    if params:
        url = f"{url}?{urllib.parse.urlencode(params)}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=15) as resp:
        payload = resp.read().decode("utf-8")
        return json.loads(payload)


def login(username, password):
    data = api_request("POST", "/auth/login", data={"username": username, "password": password})
    return data["data"]["token"]


def seed_demo_data(admin_token, teacher_token):
    submission1 = api_request(
        "POST",
        "/submissions",
        token=admin_token,
        data={"assignmentId": 1, "studentId": 3, "rawText": "Version 1: requirement analysis and database schema overview."},
    )
    time.sleep(0.3)
    submission2 = api_request(
        "POST",
        "/submissions",
        token=admin_token,
        data={"assignmentId": 1, "studentId": 3, "rawText": "Version 2: requirement analysis, schema design, and service decomposition."},
    )

    for submission in [submission1, submission2]:
        api_request(
            "POST",
            "/reviews",
            token=teacher_token,
            data={
                "submissionId": submission["data"]["id"],
                "score": 85 if submission is submission1 else 92,
                "comment": "评阅完成，建议补充对比分析",
                "dimensionScores": [
                    {"dimension": "需求完成度", "score": 88, "comment": "需求覆盖较好"},
                    {"dimension": "工程质量", "score": 90, "comment": "结构清晰"},
                    {"dimension": "文档与表达", "score": 86, "comment": "表达清楚"},
                ],
            },
        )

    task = api_request(
        "POST",
        "/plagiarism/tasks",
        token=teacher_token,
        data={"assignmentId": 1, "threshold": 0.7, "simhashWeight": 0.7, "jaccardWeight": 0.3, "maxRetry": 1, "runTimeoutSeconds": 60},
    )
    task_id = task["data"]["id"]
    for _ in range(20):
        status = api_request("GET", f"/plagiarism/tasks/{task_id}", token=teacher_token)["data"]["status"]
        if status in (2, 3, 4):
            break
        time.sleep(0.5)


def run_evaluation(admin_token):
    api_request("POST", "/plagiarism/evaluation/run", token=admin_token, params={
        "threshold": 0.7,
        "simhashWeight": 0.7,
        "jaccardWeight": 0.3,
    })
    api_request("GET", "/plagiarism/evaluation/report", token=admin_token)
    api_request("GET", "/plagiarism/evaluation/runs", token=admin_token, params={"limit": 20})


def run_perf_check_and_import(admin_token):
    subprocess.run([
        "powershell",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "./scripts/perf-check.ps1",
    ], check=True)
    perf_dir = Path("artifacts") / "perf"
    files = sorted(perf_dir.glob("perf_result_*.json"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not files:
        raise RuntimeError("perf-check did not generate output")
    data = json.loads(files[0].read_text(encoding="utf-8-sig"))
    api_request("POST", "/perf/baselines", token=admin_token, data={
        "baseUrl": data.get("baseUrl", ""),
        "path": data.get("path", ""),
        "requests": data.get("requests", 0),
        "success": data.get("success", 0),
        "failed": data.get("failed", 0),
        "errorRate": data.get("errorRate", 0),
        "minMs": data.get("minMs", 0),
        "avgMs": data.get("avgMs", 0),
        "p95Ms": data.get("p95Ms", 0),
        "maxMs": data.get("maxMs", 0),
        "generatedAt": data.get("generatedAt"),
    })


def login_ui(page, username, password):
    page.goto(f"{FRONTEND_URL}/login")
    page.wait_for_load_state("networkidle")
    page.fill("input[placeholder='请输入账号']", username)
    page.fill("input[placeholder='请输入密码']", password)
    page.click("button:has-text('登录')")
    page.wait_for_url("**/dashboard")
    page.wait_for_load_state("networkidle")


def logout_ui(page):
    page.click("button:has-text('退出登录')")
    page.wait_for_url("**/login")


def capture_screenshots():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        login_ui(page, "teacher1", "root")
        page.wait_for_timeout(800)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_notifications.png"), full_page=True)

        page.goto(f"{FRONTEND_URL}/reviews")
        page.wait_for_load_state("networkidle")
        page.click("button:has-text('Rubric配置')")
        page.wait_for_timeout(600)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_rubric_chart.png"), full_page=True)
        page.keyboard.press("Escape")

        page.goto(f"{FRONTEND_URL}/evaluation")
        page.wait_for_load_state("networkidle")
        page.click(".el-tabs__item:has-text('评估趋势')")
        page.wait_for_timeout(600)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_eval_trend.png"), full_page=True)

        page.goto(f"{FRONTEND_URL}/analytics")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(600)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_multi_dim_stats.png"), full_page=True)
        page.click(".el-tabs__item:has-text('系统指标')")
        page.wait_for_timeout(600)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_system_metrics.png"), full_page=True)
        page.click(".el-tabs__item:has-text('性能基线')")
        page.wait_for_timeout(600)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_perf_baseline.png"), full_page=True)

        logout_ui(page)
        login_ui(page, "student3", "root")
        page.goto(f"{FRONTEND_URL}/submissions?assignmentId=1&openEvolution=1")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        page.screenshot(path=str(ARTIFACT_DIR / "p2_score_evolution.png"), full_page=True)

        browser.close()


def main():
    admin_token = login("admin", "root")
    teacher_token = login("teacher1", "root")

    seed_demo_data(admin_token, teacher_token)
    run_evaluation(admin_token)
    run_perf_check_and_import(admin_token)
    capture_screenshots()


if __name__ == "__main__":
    main()
