<template>
  <AppShell title="作业管理">
    <el-card class="table-card">
      <template #header>
        <div class="table-header">
          <div>
            <div class="table-title">作业列表</div>
            <div class="table-subtitle">按课程筛选并管理作业任务</div>
          </div>
          <el-button type="primary" @click="openCreate">新建作业</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">课程ID</span>
          <el-input-number v-model="courseId" :min="1" size="small" controls-position="right" />
          <el-button size="small" type="primary" @click="loadData">加载</el-button>
          <el-tag type="info">共 {{ tableData.length }} 项</el-tag>
          <el-tag v-if="focusedAssignmentId" type="primary">已定位作业 #{{ focusedAssignmentId }}</el-tag>
          <el-button v-if="routeFromDashboard" link type="primary" @click="markAssignmentCheckedAndBack">返回工作台</el-button>
        </div>
      </div>

      <el-divider class="table-divider" />

      <el-table v-if="tableData.length > 0" :data="tableData" border stripe :row-class-name="tableRowClassName">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="deadline" label="截止时间" width="190" />
        <el-table-column prop="maxScore" label="满分" width="100" />
      </el-table>
      <el-empty v-else description="暂无作业数据，可先新建作业" />
    </el-card>

    <el-dialog v-model="showDialog" title="新建作业" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="课程ID">
          <el-input-number v-model="form.courseId" :min="1" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择时间"
          />
        </el-form-item>
        <el-form-item label="满分">
          <el-input-number v-model="form.maxScore" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="创建人ID">
          <el-input-number v-model="form.createdBy" :min="1" :disabled="true" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import AppShell from "../components/AppShell.vue";
import { createAssignmentApi, listAssignmentsApi } from "../api/modules";
import { readOptionalPositiveIntQuery, readPositiveIntQuery } from "../router/query";
import { buildDashboardReturnQuery, isFromDashboard } from "../router/dashboard-context";
import { useAuthStore } from "../stores/auth";
import { notifyApiError } from "../utils/notify";

type Assignment = {
  id: number;
  courseId: number;
  title: string;
  deadline: string;
  maxScore: number;
};

const courseId = ref(1);
const tableData = ref<Assignment[]>([]);
const showDialog = ref(false);
const focusedAssignmentId = ref<number | null>(null);
const routeFromDashboard = ref(false);
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const form = reactive({
  courseId: 1,
  title: "",
  description: "",
  deadline: "",
  maxScore: 100,
  createdBy: 1,
});

const reorderTableData = () => {
  if (!focusedAssignmentId.value) {
    return;
  }
  tableData.value = [...tableData.value].sort((left, right) => {
    if (left.id === focusedAssignmentId.value) return -1;
    if (right.id === focusedAssignmentId.value) return 1;
    return left.id - right.id;
  });
};

const loadData = async () => {
  try {
    const res = await listAssignmentsApi(courseId.value);
    tableData.value = Array.isArray(res.data) ? res.data : [];
    reorderTableData();
  } catch (error) {
    notifyApiError(error, "加载作业失败");
  }
};

const openCreate = () => {
  form.courseId = courseId.value;
  form.createdBy = authStore.user?.id || 1;
  showDialog.value = true;
};

const submitCreate = async () => {
  if (!form.title || !form.deadline) {
    ElMessage.warning("请填写标题和截止时间");
    return;
  }
  try {
    await createAssignmentApi({
      courseId: form.courseId,
      title: form.title,
      description: form.description,
      deadline: form.deadline,
      maxScore: form.maxScore,
      createdBy: form.createdBy,
    });
    ElMessage.success("创建成功");
    showDialog.value = false;
    form.title = "";
    form.description = "";
    focusedAssignmentId.value = null;
    await loadData();
  } catch (error) {
    notifyApiError(error, "创建失败");
  }
};

const tableRowClassName = ({ row }: { row: Assignment }) => {
  return row.id === focusedAssignmentId.value ? "is-focus-row" : "";
};

const backToDashboard = (options?: { handled?: string; handledCount?: number }) => {
  router.push({
    path: "/dashboard",
    query: buildDashboardReturnQuery({
      focusAssignmentId: focusedAssignmentId.value,
      focusCourseId: courseId.value,
      handled: options?.handled,
      handledCount: options?.handledCount,
    }),
  });
};

const markAssignmentCheckedAndBack = () => {
  backToDashboard({
    handled: "assignment_checked",
    handledCount: 1,
  });
};

const applyRouteQuery = async () => {
  const nextCourseId = readPositiveIntQuery(route.query.courseId, courseId.value);
  const nextAssignmentId = readOptionalPositiveIntQuery(route.query.assignmentId);
  routeFromDashboard.value = isFromDashboard(route.query);
  const shouldReload = nextCourseId !== courseId.value || tableData.value.length === 0;

  courseId.value = nextCourseId;
  focusedAssignmentId.value = nextAssignmentId;

  if (shouldReload) {
    await loadData();
    return;
  }
  reorderTableData();
};

watch([() => route.query.courseId, () => route.query.assignmentId], async () => {
  await applyRouteQuery();
}, { immediate: true });
</script>

<style scoped>
.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.table-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 0 4px;
}

.filter-left {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 12px;
  color: #64748b;
}

.table-divider {
  margin: 8px 0 12px;
}

:deep(.el-table .is-focus-row) {
  --el-table-tr-bg-color: #ecf5ff;
}

:deep(.el-table th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}
</style>
