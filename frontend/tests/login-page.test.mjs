import assert from "node:assert/strict";

import { createDefaultLoginForm, validateLoginForm } from "../.test-dist/src/views/login.logic.js";

export function runLoginPageTests() {
  assert.deepEqual(createDefaultLoginForm(), {
    username: "admin",
    password: "123456",
  });

  assert.equal(validateLoginForm({ username: "", password: "123456" }), "请输入账号和密码");
  assert.equal(validateLoginForm({ username: "admin", password: "" }), "请输入账号和密码");
  assert.equal(validateLoginForm({ username: "admin", password: "123456" }), null);
}
