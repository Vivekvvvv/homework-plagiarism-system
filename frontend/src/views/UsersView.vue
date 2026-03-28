<template>
  <AppShell title="用户管理">
    <div class="edu-page">
      <div class="edu-toolbar">
        <div>
          <div class="edu-toolbar__title">用户列表</div>
          <div class="edu-toolbar__sub">管理所有用户账号，仅管理员可访问</div>
        </div>
      </div>

      <div class="edu-card">
        <el-table v-if="users.length > 0" :data="users" border stripe class="edu-table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'primary'"
                :disabled="row.username === currentUsername"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无用户数据" />
      </div>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import AppShell from "../components/AppShell.vue";
import { listUsersApi, toggleUserStatusApi } from "../api/users";
import { useAuthStore } from "../stores/auth";
import { notifyApiError } from "../utils/notify";

const authStore = useAuthStore();
const currentUsername = computed(() => authStore.user?.username);

const users = ref<any[]>([]);

const roleLabel = (role: string) => {
  const r = (role || "").toLowerCase();
  if (r === "admin") return "管理员";
  if (r === "teacher") return "教师";
  return "学生";
};

const roleTagType = (role: string) => {
  const r = (role || "").toLowerCase();
  if (r === "admin") return "danger";
  if (r === "teacher") return "warning";
  return "";
};

const loadData = async () => {
  const res = await listUsersApi();
  users.value = res.data;
};

const toggleStatus = async (row: any) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1;
    await toggleUserStatusApi(row.id, newStatus);
    row.status = newStatus;
    ElMessage.success(newStatus === 1 ? "已启用" : "已禁用");
  } catch (error) {
    notifyApiError(error, "操作失败");
  }
};

onMounted(async () => {
  try {
    await loadData();
  } catch (error) {
    notifyApiError(error, "加载用户列表失败");
  }
});
</script>
