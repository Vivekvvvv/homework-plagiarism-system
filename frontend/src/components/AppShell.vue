<template>
  <el-container class="shell">
    <el-aside class="shell__aside" width="232px">
      <div class="shell__logo">
        <div class="shell__logo-title">作业查重与评阅辅助系统</div>
        <div class="shell__logo-subtitle">Homework Insight Suite</div>
      </div>
      <el-menu class="shell__menu" :default-active="activePath" router>
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item v-if="isTeacherOrAdmin" index="/courses">课程管理</el-menu-item>
        <el-menu-item v-if="isTeacherOrAdmin" index="/assignments">作业管理</el-menu-item>
        <el-menu-item index="/submissions">作业提交</el-menu-item>
        <el-menu-item v-if="isTeacherOrAdmin" index="/plagiarism">查重任务</el-menu-item>
        <el-menu-item v-if="isTeacherOrAdmin" index="/reviews">评阅中心</el-menu-item>
        <el-menu-item v-if="isTeacherOrAdmin" index="/evaluation">评估与审计</el-menu-item>
        <el-menu-item v-if="isTeacherOrAdmin" index="/analytics">数据看板</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="shell__content">
      <el-header class="shell__header">
        <div class="shell__title">
          <span>{{ title }}</span>
          <span class="shell__title-hint">实时协作 · 统一审计 · 数据可视</span>
        </div>
        <div class="shell__user">
          <span class="shell__user-text">{{ userText }}</span>
          <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="shell__main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

defineProps<{ title: string }>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const activePath = computed(() => route.path);
const isTeacherOrAdmin = computed(() => {
  const role = (authStore.user?.role || "").toUpperCase();
  return role === "ADMIN" || role === "TEACHER";
});
const userText = computed(() => {
  const name = authStore.user?.realName || authStore.user?.username || "未登录";
  const role = authStore.user?.role ? ` (${authStore.user.role})` : "";
  return `${name}${role}`;
});

const handleLogout = () => {
  authStore.logout();
  router.push("/login");
};
</script>

<style scoped>
.shell {
  min-height: 100vh;
}

.shell__aside {
  border-right: 1px solid var(--app-border);
  background: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%);
  box-shadow: inset -1px 0 0 rgba(15, 23, 42, 0.04);
}

.shell__logo {
  padding: 20px 16px 16px;
  border-bottom: 1px solid var(--app-border);
}

.shell__logo-title {
  font-weight: 700;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.3;
}

.shell__logo-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid var(--app-border);
  backdrop-filter: blur(12px);
}

.shell__content {
  background: transparent;
}

.shell__menu {
  border-right: none;
  background: transparent;
  padding: 8px 8px 12px;
}

.shell__title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.shell__title > span:first-child {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.shell__title-hint {
  font-size: 12px;
  color: #64748b;
}

.shell__user {
  display: flex;
  gap: 12px;
  align-items: center;
}

.shell__user-text {
  font-size: 13px;
  color: #334155;
}

.shell__main {
  padding: 20px 20px 28px;
}
</style>
