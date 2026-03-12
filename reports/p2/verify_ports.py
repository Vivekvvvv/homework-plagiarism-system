import json
import urllib.request
import urllib.error


def read_url(url, request=None):
    try:
        resp = urllib.request.urlopen(request or url, timeout=10)
        return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


health_status, health_body = read_url("http://localhost:8081/actuator/health")
print("health", health_status, health_body)

login_req = urllib.request.Request(
    "http://localhost:8081/api/v1/auth/login",
    data=json.dumps({"username": "admin", "password": "root"}).encode(),
    headers={"Content-Type": "application/json"},
    method="POST",
)
login_status, login_body = read_url("http://localhost:8081/api/v1/auth/login", login_req)
print("login", login_status, login_body)
