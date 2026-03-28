<template>
  <el-container class="shell">
    <!-- 侧边栏 -->
    <el-aside class="shell__aside" width="240px">
      <div class="shell__logo">
        <div class="shell__logo-icon">📚</div>
        <div>
          <div class="shell__logo-title">作业查重与评阅</div>
          <div class="shell__logo-subtitle">智能洞察 · 公正评阅</div>
        </div>
      </div>

      <nav class="shell__nav">
        <router-link class="shell__nav-item" to="/dashboard">
          <span class="shell__nav-icon">🏠</span>
          <span>仪表盘</span>
        </router-link>
        <router-link v-if="isTeacherOrAdmin" class="shell__nav-item" to="/courses">
          <span class="shell__nav-icon">🎓</span>
          <span>课程管理</span>
        </router-link>
        <router-link v-if="isTeacherOrAdmin" class="shell__nav-item" to="/assignments">
          <span class="shell__nav-icon">📝</span>
          <span>作业管理</span>
        </router-link>
        <router-link class="shell__nav-item" to="/submissions">
          <span class="shell__nav-icon">📤</span>
          <span>作业提交</span>
        </router-link>
        <router-link v-if="isTeacherOrAdmin" class="shell__nav-item" to="/plagiarism">
          <span class="shell__nav-icon">🔍</span>
          <span>查重任务</span>
        </router-link>
        <router-link v-if="isTeacherOrAdmin" class="shell__nav-item" to="/reviews">
          <span class="shell__nav-icon">✏️</span>
          <span>评阅中心</span>
        </router-link>
        <router-link v-if="isTeacherOrAdmin" class="shell__nav-item" to="/evaluation">
          <span class="shell__nav-icon">📊</span>
          <span>评估与审计</span>
        </router-link>
        <router-link v-if="isTeacherOrAdmin" class="shell__nav-item" to="/analytics">
          <span class="shell__nav-icon">📈</span>
          <span>数据看板</span>
        </router-link>

        <div class="shell__nav-divider"></div>

        <router-link class="shell__nav-item" to="/change-password">
          <span class="shell__nav-icon">🔑</span>
          <span>修改密码</span>
        </router-link>
      </nav>

      <div class="shell__aside-footer">
        <div class="shell__user-card">
          <div class="shell__user-avatar">{{ userInitial }}</div>
          <div class="shell__user-info">
            <div class="shell__user-name">{{ authStore.user?.username }}</div>
            <div class="shell__user-role">{{ roleLabel }}</div>
          </div>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="shell__body">
      <el-header class="shell__header" height="64px">
        <div class="shell__header-left">
          <div class="shell__page-title">{{ title }}</div>
          <div class="shell__page-hint">实时协作 · 统一审计 · 数据可视</div>
        </div>
        <div class="shell__header-right">
          <button class="shell__bell" @click="resetBadge" title="通知">
            <span class="shell__bell-icon">🔔</span>
            <span v-if="unreadBadge > 0" class="shell__badge">{{ unreadBadge > 99 ? '99+' : unreadBadge }}</span>
          </button>
          <el-button class="shell__logout-btn" size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="shell__main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElNotification } from "element-plus";
import { useAuthStore } from "../stores/auth";
import { connectWs, closeWs, unreadBadge, resetBadge, type WsNotification } from "../utils/ws";

defineProps<{ title: string }>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const activePath = computed(() => route.path);
const isTeacherOrAdmin = computed(() => {
  const role = authStore.user?.role;
  return role === "teacher" || role === "admin";
});

const userText = computed(() => {
  const user = authStore.user;
  if (!user) return "";
  return `${user.username} (${user.role})`;
});

const userInitial = computed(() => {
  const name = authStore.user?.username || "?";
  return name.charAt(0).toUpperCase();
});

const roleLabel = computed(() => {
  const role = authStore.user?.role;
  if (role === "teacher") return "教师";
  if (role === "admin") return "管理员";
  return "学生";
});

const handleLogout = () => {
  authStore.logout();
  router.push("/login");
};

let onMessage: ((n: WsNotification) => void) | null = null;

onMounted(() => {
  if (authStore.token) {
    onMessage = (n: WsNotification) => {
      ElNotification({
        title: n.title,
        message: n.content,
        type: "info",
        position: "bottom-right",
      });
    };
    connectWs(authStore.token, onMessage);
  }
});

onBeforeUnmount(() => {
  closeWs();
});
</script>

<style scoped>
/* ── 设计令牌 ── */
.shell {
  --edu-green: #16a37f;
  --edu-green-light: #e6f7f3;
  --edu-green-mid: #b2e8d8;
  --edu-blue: #2563eb;
  --edu-blue-light: #eff6ff;
  --edu-sidebar-bg: #ffffff;
  --edu-sidebar-width: 240px;
  --edu-bg: #f4faf8;
  --edu-border: #e2ede9;
  --edu-text-main: #1a2e26;
  --edu-text-sub: #6b8f82;
  --edu-radius: 12px;
  --edu-shadow-card: 0 2px 12px rgba(22, 163, 127, 0.08);
  min-height: 100vh;
  background: var(--edu-bg);
  font-family: "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", sans-serif;
}

/* ── 侧边栏 ── */
.shell__aside {
  background: var(--edu-sidebar-bg);
  border-right: 1px solid var(--edu-border);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 2px 0 16px rgba(22, 163, 127, 0.06);
}

/* Logo 区 */
.shell__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 18px;
  border-bottom: 1px solid var(--edu-border);
  background: linear-gradient(135deg, #f0fdf8 0%, #ffffff 100%);
}

.shell__logo-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}

.shell__logo-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--edu-green);
  line-height: 1.3;
  letter-spacing: 0.3px;
}

.shell__logo-subtitle {
  margin-top: 3px;
  font-size: 11px;
  color: var(--edu-text-sub);
  letter-spacing: 0.5px;
}

/* 导航 */
.shell__nav {
  flex: 1;
  padding: 12px 10px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.shell__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 13.5px;
  color: var(--edu-text-main);
  text-decoration: none;
  transition: background 0.18s, color 0.18s;
  font-weight: 500;
  cursor: pointer;
}

.shell__nav-item:hover {
  background: var(--edu-green-light);
  color: var(--edu-green);
}

.shell__nav-item.router-link-active {
  background: var(--edu-green-light);
  color: var(--edu-green);
  font-weight: 600;
  box-shadow: inset 3px 0 0 var(--edu-green);
}

.shell__nav-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.shell__nav-divider {
  height: 1px;
  background: var(--edu-border);
  margin: 8px 4px;
}

/* 用户信息底部 */
.shell__aside-footer {
  padding: 12px 10px 16px;
  border-top: 1px solid var(--edu-border);
}

.shell__user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--edu-green-light);
  border-radius: 10px;
}

.shell__user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--edu-green), #0891b2);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.shell__user-info {
  overflow: hidden;
}

.shell__user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--edu-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shell__user-role {
  font-size: 11px;
  color: var(--edu-green);
  margin-top: 1px;
}

/* ── 主体 ── */
.shell__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--edu-bg);
}

/* Header */
.shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid var(--edu-border);
  backdrop-filter: blur(10px);
  padding: 0 24px;
  box-shadow: 0 1px 8px rgba(22, 163, 127, 0.06);
}

.shell__header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.shell__page-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--edu-text-main);
  letter-spacing: 0.2px;
}

.shell__page-hint {
  font-size: 11px;
  color: var(--edu-text-sub);
}

.shell__header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.shell__bell {
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: background 0.15s;
  line-height: 1;
}

.shell__bell:hover {
  background: var(--edu-green-light);
}

.shell__bell-icon {
  font-size: 20px;
  display: block;
}

.shell__badge {
  position: absolute;
  top: -3px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 700;
  pointer-events: none;
}

.shell__logout-btn {
  border-color: var(--edu-border) !important;
  color: var(--edu-text-sub) !important;
  background: #fff !important;
  border-radius: 8px !important;
  transition: all 0.15s !important;
}

.shell__logout-btn:hover {
  border-color: #ef4444 !important;
  color: #ef4444 !important;
  background: #fff5f5 !important;
}

/* 主内容区 */
.shell__main {
  padding: 24px;
  flex: 1;
}

/* Element Plus 菜单覆盖（兜底，实际菜单已用自定义 router-link 替代） */
:deep(.el-menu) {
  border-right: none !important;
}
</style>
