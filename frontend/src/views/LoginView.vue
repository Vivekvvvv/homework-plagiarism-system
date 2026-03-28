<template>
  <div class="login-bg">
    <!-- 装饰圆圈 -->
    <div class="login-bg__circle login-bg__circle--1"></div>
    <div class="login-bg__circle login-bg__circle--2"></div>

    <div class="login-wrap">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="login-brand__icon">📚</div>
        <h1 class="login-brand__title">作业查重与评阅系统</h1>
        <p class="login-brand__desc">智能化作业管理平台，支持文本/代码查重、教师批注、实时通知与多维数据分析。</p>
        <ul class="login-brand__features">
          <li>🔍 SimHash + 文本相似度双引擎查重</li>
          <li>✏️ 灵活 Rubric 评阅 · 批量打分</li>
          <li>📊 多维度数据看板 · 可视化分析</li>
          <li>�� WebSocket 实时推送通知</li>
        </ul>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="login-card">
        <div class="login-card__header">
          <div class="login-card__logo">🎓</div>
          <h2 class="login-card__title">欢迎登录</h2>
          <p class="login-card__sub">请输入您的账号和密码</p>
        </div>

        <el-form :model="form" class="login-form" @keyup.enter="handleLogin">
          <div class="login-field">
            <label class="login-field__label">账号</label>
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              size="large"
              class="login-input"
            >
              <template #prefix><span style="font-size:15px">👤</span></template>
            </el-input>
          </div>
          <div class="login-field">
            <label class="login-field__label">密码</label>
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              size="large"
              show-password
              class="login-input"
            >
              <template #prefix><span style="font-size:15px">🔒</span></template>
            </el-input>
          </div>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登  录' }}
          </el-button>
        </el-form>

        <div class="login-hint">默认账号：<code>admin</code> / <code>123456</code></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { createDefaultLoginForm, validateLoginForm } from "./login.logic";
import { notifyApiError } from "../utils/notify";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);

const form = reactive(createDefaultLoginForm());

const handleLogin = async () => {
  const validationMessage = validateLoginForm(form);
  if (validationMessage) {
    ElMessage.warning(validationMessage);
    return;
  }
  loading.value = true;
  try {
    await authStore.login(form.username, form.password);
    ElMessage.success("登录成功");
    router.push("/dashboard");
  } catch (error) {
    notifyApiError(error, "登录失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0fdf8 0%, #e0f2fe 50%, #f0fdf8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 24px;
}

.login-bg__circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.18;
  pointer-events: none;
}

.login-bg__circle--1 {
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, #16a37f, transparent 70%);
  top: -140px;
  right: -100px;
}

.login-bg__circle--2 {
  width: 380px;
  height: 380px;
  background: radial-gradient(circle, #2563eb, transparent 70%);
  bottom: -100px;
  left: -80px;
}

.login-wrap {
  display: flex;
  align-items: center;
  gap: 60px;
  max-width: 900px;
  width: 100%;
  z-index: 1;
}

/* 品牌区 */
.login-brand {
  flex: 1;
  color: #1a2e26;
}

.login-brand__icon {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
}

.login-brand__title {
  font-size: 26px;
  font-weight: 800;
  color: #16a37f;
  margin: 0 0 12px;
  line-height: 1.2;
}

.login-brand__desc {
  font-size: 14px;
  color: #4b6b60;
  line-height: 1.7;
  margin-bottom: 24px;
}

.login-brand__features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.login-brand__features li {
  font-size: 13.5px;
  color: #2d5748;
  background: rgba(22, 163, 127, 0.09);
  padding: 9px 14px;
  border-radius: 10px;
  font-weight: 500;
}

/* 登录卡片 */
.login-card {
  width: 360px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 20px;
  padding: 36px 32px;
  box-shadow: 0 8px 40px rgba(22, 163, 127, 0.12), 0 2px 8px rgba(0,0,0,0.06);
  border: 1px solid #e2ede9;
}

.login-card__header {
  text-align: center;
  margin-bottom: 28px;
}

.login-card__logo {
  font-size: 40px;
  margin-bottom: 10px;
}

.login-card__title {
  font-size: 22px;
  font-weight: 800;
  color: #1a2e26;
  margin: 0 0 6px;
}

.login-card__sub {
  font-size: 13px;
  color: #6b8f82;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-field__label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: #2d5748;
  margin-bottom: 6px;
  letter-spacing: 0.3px;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px #c8dfd7 !important;
  transition: box-shadow 0.18s !important;
}

.login-input :deep(.el-input__wrapper:hover),
.login-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #16a37f !important;
}

.login-btn {
  width: 100%;
  border-radius: 10px !important;
  background: linear-gradient(90deg, #16a37f, #0891b2) !important;
  border: none !important;
  font-size: 15px !important;
  font-weight: 700 !important;
  letter-spacing: 2px !important;
  height: 44px !important;
  margin-top: 4px;
  transition: opacity 0.18s, transform 0.15s !important;
}

.login-btn:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.login-hint {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.login-hint code {
  background: #f0fdf8;
  color: #16a37f;
  padding: 1px 6px;
  border-radius: 5px;
  font-family: monospace;
  font-weight: 600;
}

@media (max-width: 700px) {
  .login-wrap { flex-direction: column; gap: 28px; }
  .login-brand { display: none; }
  .login-card { width: 100%; }
}
</style>
