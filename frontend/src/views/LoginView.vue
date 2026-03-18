<template>
  <div class="login">
    <el-card class="login__card">
      <template #header>
        <div class="login__title">系统登录</div>
      </template>
      <el-form :model="form" label-width="64px">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="login__hint">默认账号：admin / 123456</div>
    </el-card>
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
.login {
  width: 100%;
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(800px 400px at 30% 20%, rgba(79, 70, 229, 0.08), transparent 60%),
    radial-gradient(600px 300px at 70% 80%, rgba(139, 92, 246, 0.06), transparent 50%),
    linear-gradient(130deg, #f2f6ff 0%, #eef7ff 40%, #f8f9ff 100%);
}

.login__card {
  width: 420px;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.10), 0 4px 12px rgba(15, 23, 42, 0.04);
}

.login__title {
  font-weight: 700;
  color: var(--app-accent, #4f46e5);
  font-size: 18px;
}

.login__hint {
  color: #667085;
  font-size: 13px;
}
</style>
