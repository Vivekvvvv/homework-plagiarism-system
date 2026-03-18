<template>
  <AppShell title="修改密码">
    <el-card class="change-password">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="至少8位，含大小写字母+数字+特殊字符"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">确认修改</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        title="密码强度要求"
        type="info"
        :closable="false"
        style="margin-top: 20px"
      >
        <ul style="margin: 0; padding-left: 20px">
          <li>长度至少 8 位</li>
          <li>必须包含大写字母</li>
          <li>必须包含小写字母</li>
          <li>必须包含数字</li>
          <li>必须包含特殊字符（如 !@#$%^&*）</li>
        </ul>
      </el-alert>
    </el-card>
  </AppShell>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { useRouter } from "vue-router";
import AppShell from "../components/AppShell.vue";
import { changePasswordApi } from "../api/auth";
import { notifyApiError } from "../utils/notify";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const validatePassword = (_rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error("请输入新密码"));
  }
  if (value.length < 8) {
    return callback(new Error("密码长度至少 8 位"));
  }
  if (!/[A-Z]/.test(value)) {
    return callback(new Error("密码必须包含大写字母"));
  }
  if (!/[a-z]/.test(value)) {
    return callback(new Error("密码必须包含小写字母"));
  }
  if (!/[0-9]/.test(value)) {
    return callback(new Error("密码必须包含数字"));
  }
  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value)) {
    return callback(new Error("密码必须包含特殊字符"));
  }
  callback();
};

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error("请再次输入新密码"));
  }
  if (value !== form.newPassword) {
    return callback(new Error("两次输入的密码不一致"));
  }
  callback();
};

const rules: FormRules = {
  oldPassword: [{ required: true, message: "请输入旧密码", trigger: "blur" }],
  newPassword: [{ required: true, validator: validatePassword, trigger: "blur" }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: "blur" }],
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  loading.value = true;
  try {
    await changePasswordApi({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    });
    ElMessage.success("密码修改成功，请重新登录");
    authStore.logout();
    router.push("/login");
  } catch (error: any) {
    notifyApiError(error, "密码修改失败");
  } finally {
    loading.value = false;
  }
};

const handleReset = () => {
  formRef.value?.resetFields();
};
</script>

<style scoped>
.change-password {
  max-width: 600px;
  margin: 0 auto;
}
</style>
