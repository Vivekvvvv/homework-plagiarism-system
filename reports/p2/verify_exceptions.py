import json
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8081/api/v1"


def api_request(method, path, token=None, data=None, params=None):
    url = f"{BASE_URL}{path}"
    if params:
        qs = "&".join(f"{k}={v}" for k, v in params.items())
        url = f"{url}?{qs}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))


def login(username, password):
    _, data = api_request("POST", "/auth/login", data={"username": username, "password": password})
    return data["data"]["token"]


def assert_fail(label, status, payload):
    if status == 200 and payload.get("code") == 0:
        raise RuntimeError(f"{label} should fail but succeeded")


def main():
    token = login("teacher1", "root")

    # 查重异常：不存在的作业
    status, payload = api_request("POST", "/plagiarism/tasks", token=token, data={"assignmentId": 999999, "threshold": 0.7})
    assert_fail("plagiarism invalid assignment", status, payload)

    # 查重异常：取消不存在任务
    status, payload = api_request("PATCH", "/plagiarism/tasks/999999/cancel", token=token)
    assert_fail("plagiarism cancel invalid task", status, payload)

    # 评阅异常：score为空且dimensionScores为空
    status, payload = api_request("POST", "/reviews", token=token, data={"submissionId": 1})
    assert_fail("review missing score", status, payload)

    # 评阅异常：score越界
    status, payload = api_request("POST", "/reviews", token=token, data={"submissionId": 1, "score": 200})
    assert_fail("review score overflow", status, payload)

    print("异常场景验证通过")


if __name__ == "__main__":
    main()
