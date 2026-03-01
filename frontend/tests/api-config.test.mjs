import assert from "node:assert/strict";

import { API_BASE_URL, buildApiUrl, buildAuthHeaders, getStoredToken } from "../.test-dist/src/api/config.js";

export function runApiConfigTests() {
  assert.equal(API_BASE_URL, "http://localhost:8081/api/v1");
  assert.equal(buildApiUrl("/reviews/export"), "http://localhost:8081/api/v1/reviews/export");
  assert.equal(buildApiUrl("plagiarism/tasks"), "http://localhost:8081/api/v1/plagiarism/tasks");

  assert.deepEqual(buildAuthHeaders("jwt-token"), {
    Authorization: "Bearer jwt-token",
  });
  assert.deepEqual(buildAuthHeaders(""), {});

  const storage = {
    getItem(key) {
      return key === "token" ? "stored-token" : null;
    },
  };
  assert.equal(getStoredToken(storage), "stored-token");
  assert.equal(getStoredToken({ getItem: () => null }), "");
}
