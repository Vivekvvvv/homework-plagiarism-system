import assert from "node:assert/strict";

import { readOptionalPositiveIntQuery, readPositiveIntQuery, readStringQuery } from "../.test-dist/src/router/query.js";

export function runRouterQueryTests() {
  assert.equal(readPositiveIntQuery("12", 1), 12);
  assert.equal(readPositiveIntQuery(["9"], 1), 9);
  assert.equal(readPositiveIntQuery("0", 7), 7);
  assert.equal(readPositiveIntQuery("abc", 7), 7);

  assert.equal(readOptionalPositiveIntQuery("18"), 18);
  assert.equal(readOptionalPositiveIntQuery(["3"]), 3);
  assert.equal(readOptionalPositiveIntQuery(""), null);
  assert.equal(readOptionalPositiveIntQuery("-1"), null);

  assert.equal(readStringQuery("dashboard"), "dashboard");
  assert.equal(readStringQuery([" reviews "]), "reviews");
  assert.equal(readStringQuery("   "), null);
}
