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
        <router-link v-if="isAdmin" class="shell__nav-item" to="/users">
          <span class="shell__nav-icon">👥</span>
          <span>用户管理</span>
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
          <!-- 通知铃铛 -->
          <el-popover
            v-model:visible="notifPanelVisible"
            trigger="click"
            placement="bottom-end"
            :width="380"
            popper-class="notif-popover"
          >
            <template #reference>
              <button class="shell__bell" title="通知">
                <span class="shell__bell-icon">🔔</span>
                <span v-if="unreadBadge > 0" class="shell__badge">{{ unreadBadge > 99 ? '99+' : unreadBadge }}</span>
              </button>
            </template>

            <!-- 通知面板 -->
            <div class="notif-panel">
              <div class="notif-panel__header">
                <span class="notif-panel__title">通知消息</span>
                <div class="notif-panel__actions">
                  <el-button link size="small" @click="markAllRead">全部已读</el-button>
                  <el-button link size="small" @click="loadNotifications">刷新</el-button>
                </div>
              </div>

              <!-- 发布公告（仅 admin/teacher） -->
              <div v-if="isTeacherOrAdmin" class="notif-panel__broadcast">
                <el-divider content-position="left"><span style="font-size:12px">发布公告</span></el-divider>
                <el-input v-model="broadcastForm.title" placeholder="公告标题" size="small" style="margin-bottom:6px" />
                <el-input v-model="broadcastForm.content" type="textarea" placeholder="公告内容（可选）" size="small" :rows="2" style="margin-bottom:6px" />
                <div style="display:flex;gap:6px;align-items:center">
                  <el-select v-model="broadcastForm.level" size="small" style="width:90px">
                    <el-option label="普通" value="info" />
                    <el-option label="警告" value="warning" />
                    <el-option label="紧急" value="danger" />
                    <el-option label="成功" value="success" />
                  </el-select>
                  <el-select v-model="broadcastForm.target" size="small" style="width:90px">
                    <el-option label="全体" value="all" />
                    <el-option label="学生" value="student" />
                    <el-option label="教师" value="teacher" />
                  </el-select>
                  <el-button type="primary" size="small" :loading="broadcasting" @click="sendBroadcast">发布</el-button>
                </div>
              </div>

              <el-divider style="margin:8px 0" />

              <!-- 通知列表 -->
              <div v-if="notifLoading" style="text-align:center;padding:16px">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <div v-else-if="notifications.length === 0" style="text-align:center;color:#aaa;padding:16px;font-size:13px">暂无通知</div>
              <div v-else class="notif-list">
                <div
                  v-for="n in notifications"
                  :key="n.id"
                  class="notif-item"
                  :class="{ 'notif-item--unread': n.status === 0 }"
                >
                  <div class="notif-item__dot" :class="'notif-item__dot--' + n.level"></div>
                  <div class="notif-item__body">
                    <div class="notif-item__title">{{ n.title }}</div>
                    <div v-if="n.content" class="notif-item__content">{{ n.content }}</div>
                    <div class="notif-item__time">{{ formatTime(n.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>

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
import { computed, onMounted, onBeforeUnmount, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElNotification, ElMessage } from "element-plus";
import { Loading } from "@element-plus/icons-vue";
import { useAuthStore } from "../stores/auth";
import { connectWs, closeWs, unreadBadge, resetBadge, type WsNotification } from "../utils/ws";
import {
  listNotificationsApi,
  markNotificationsReadApi,
  broadcastNotificationApi,
} from "../api/notifications";

defineProps<{ title: string }>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const activePath = computed(() => route.path);
const isTeacherOrAdmin = computed(() => {
  const role = authStore.user?.role?.toLowerCase();
  return role === "teacher" || role === "admin";
});

const isAdmin = computed(() => authStore.user?.role?.toLowerCase() === "admin");

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
  const role = authStore.user?.role?.toLowerCase();
  if (role === "teacher") return "教师";
  if (role === "admin") return "管理员";
  return "学生";
});

const handleLogout = () => {
  authStore.logout();
  router.push("/login");
};

// ── 通知面板 ──
const notifPanelVisible = ref(false);
const notifLoading = ref(false);
const notifications = ref<any[]>([]);

const broadcastForm = ref({ title: "", content: "", level: "info", target: "all" });
const broadcasting = ref(false);

const loadNotifications = async () => {
  notifLoading.value = true;
  try {
    const res = await listNotificationsApi({ limit: 30 });
    notifications.value = res.data ?? [];
  } catch {
    // ignore
  } finally {
    notifLoading.value = false;
  }
};

const markAllRead = async () => {
  try {
    await markNotificationsReadApi({ all: true });
    notifications.value.forEach((n) => (n.status = 1));
    resetBadge();
  } catch {
    ElMessage.error("操作失败");
  }
};

const sendBroadcast = async () => {
  if (!broadcastForm.value.title.trim()) {
    ElMessage.warning("请填写公告标题");
    return;
  }
  broadcasting.value = true;
  try {
    const res = await broadcastNotificationApi(broadcastForm.value);
    ElMessage.success(`已发送给 ${res.data} 名用户`);
    broadcastForm.value.title = "";
    broadcastForm.value.content = "";
  } catch {
    ElMessage.error("发布失败");
  } finally {
    broadcasting.value = false;
  }
};

const formatTime = (val: string | null) => {
  if (!val) return "";
  const d = new Date(val);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
};

watch(notifPanelVisible, (v) => {
  if (v) loadNotifications();
});

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

/* ── 通知面板 ── */
.notif-panel {
  max-height: 480px;
  display: flex;
  flex-direction: column;
}

.notif-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.notif-panel__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--edu-text-main);
}

.notif-panel__actions {
  display: flex;
  gap: 4px;
}

.notif-panel__broadcast {
  margin-bottom: 4px;
}

.notif-list {
  overflow-y: auto;
  max-height: 240px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.notif-item {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid transparent;
  transition: background 0.15s;
}

.notif-item--unread {
  background: #eff9f6;
  border-color: #d1fae5;
}

.notif-item__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 5px;
  background: #94a3b8;
}

.notif-item__dot--info    { background: #3b82f6; }
.notif-item__dot--warning { background: #f59e0b; }
.notif-item__dot--danger  { background: #ef4444; }
.notif-item__dot--success { background: #10b981; }

.notif-item__body {
  flex: 1;
  min-width: 0;
}

.notif-item__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--edu-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notif-item__content {
  font-size: 12px;
  color: var(--edu-text-sub);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notif-item__time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 3px;
}
</style>
